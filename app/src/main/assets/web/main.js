import * as THREE from "three";
import { GLTFLoader } from "three/addons/loaders/GLTFLoader.js";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";
import { GUI } from "three/addons/libs/lil-gui.module.min.js";

const app = document.getElementById("app");
const isDevPanelEnabled = new URLSearchParams(window.location.search).get("dev") === "1";

const scene = new THREE.Scene();
scene.background = null;

const camera = new THREE.PerspectiveCamera(40, window.innerWidth / window.innerHeight, 0.1, 200);
// Default view: oblique ~45° looking at the ground.
camera.position.set(-35, 25, 40); 
camera.lookAt(-2,0, -20);

const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setClearColor(0x000000, 0);
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
app.appendChild(renderer.domElement);

// Lock camera interaction unless dev mode is enabled (?dev=1).
const controls = isDevPanelEnabled ? new OrbitControls(camera, renderer.domElement) : null;
if (controls) {
  controls.enableDamping = true;
  controls.target.set(0, 0.6, 0);
}

scene.add(new THREE.HemisphereLight(0xffffff, 0x334455, 1.35));
const sun = new THREE.DirectionalLight(0xffffff, 1.05);
// Placeholder until GLB loads; then aligned to model center + overhead offset.
sun.position.set(0, 18, 2);
sun.castShadow = true;
// Shadow "square" edge appears when the ortho shadow frustum is too small and its
// boundary becomes visible on the ground. Start with a generous extent, then
// fit it to the model after GLB loads.
let shadowExtent = 60;
sun.shadow.mapSize.set(2048, 2048);
sun.shadow.camera.near = 0.4;
sun.shadow.camera.far = 140;
sun.shadow.camera.left = -shadowExtent;
sun.shadow.camera.right = shadowExtent;
sun.shadow.camera.top = shadowExtent;
sun.shadow.camera.bottom = -shadowExtent;
sun.shadow.bias = -0.00025;
sun.shadow.normalBias = 0.035;
sun.shadow.radius = 3.5;
scene.add(sun);
scene.add(sun.target);

// Grid floor: frosted-glass-ish dark gray plane + subtle grid lines.
// Plane exists mainly to receive shadows; grid provides the "raster" look.
// Ground "map" size (plane + grid). Increase to show more area.
const groundSize = 80;
const groundGeo = new THREE.PlaneGeometry(groundSize, groundSize);
const groundMat = new THREE.MeshPhysicalMaterial({
  color: 0x76684a,
  roughness: 0.92,
  metalness: 0,
  transparent: true,
  opacity: 0.28,
  transmission: 0.25,
  thickness: 0.8,
  ior: 1.35
});
const ground = new THREE.Mesh(groundGeo, groundMat);
ground.rotation.x = -Math.PI / 2;
ground.position.y = -0.02;
ground.receiveShadow = true;
scene.add(ground);

// Keep grid cell density roughly consistent when scaling groundSize.
const grid = new THREE.GridHelper(groundSize, 160, 0x6f7886, 0x404652);
grid.position.y = -0.019; // slightly above plane to avoid z-fighting
grid.material.transparent = true;
grid.material.opacity = 0.45;
grid.material.depthWrite = false;
scene.add(grid);

if (isDevPanelEnabled) {
  const axes = new THREE.AxesHelper(2.5);
  scene.add(axes);
}

const state = {
  main: {
    roll: 0,
    pitch: 0,
    yaw: 0
  },
  lengths: {
    boom: 1,
    stick: 1
  },
  joints: {
    base: { x: 0, y: 0, z: 0 },
    boom: { x: 0, y: 0, z: 0 },
    stick: { x: 0, y: 0, z: 0 },
    bucket: { x: 0, y: 0, z: 0 }
  }
};

const nodes = {
  main: null,
  car: null,
  armature: null,
  base: null,
  boom: null,
  stick: null,
  bucket: null,
  driverCabin: null,
  track: null,
  diggingBucket: null
};

const baseScale = {
  boom: new THREE.Vector3(1, 1, 1),
  stick: new THREE.Vector3(1, 1, 1)
};

const lengthAxis = {
  boom: "y",
  stick: "y"
};

const partColors = {
  theme: 0x004dff,
  // Whiter/lighter variant for the car body — same hue, blended toward white.
  themeBody: 0x80aaff
};

function degToRad(value) {
  return THREE.MathUtils.degToRad(value);
}

// IMU protocol (per embedded team):
//  - Each IMU already reports the LOCAL angle relative to its parent segment
//    (boom vs cab, stick vs boom, bucket vs stick). No subtraction needed.
//  - Cab is the reference at 0°, everything downstream is derived from it,
//    so no rest-pose calibration offset is required either.
//  - Range: -180° ~ +180°, right-hand rule.
// Per-joint config: only a sign flip in case Three.js axis points opposite
// to IMU's right-hand-rule positive direction.  rotZ = sign * imuValue
const IMU_CONFIG = {
  boom:   { sign: -1 },
  stick:  { sign: -1 },
  bucket: { sign: 1 }
};

function imuToLocalAngle(jointName, imuValue) {
  const cfg = IMU_CONFIG[jointName];
  if (!cfg) return imuValue;
  return cfg.sign * imuValue;
}

// Visible on-screen debug overlay (enable with ?debug=1).
// Shows the latest raw IMU input and the resulting rotZ actually being applied.
const isDebugOverlayEnabled = new URLSearchParams(window.location.search).get("debug") === "1";
const debugOverlay = (() => {
  if (!isDebugOverlayEnabled) return { update() {}, setSource() {} };
  const el = document.createElement("div");
  el.style.cssText = [
    "position:fixed", "top:8px", "left:8px", "z-index:9999",
    "padding:8px 10px", "background:rgba(0,0,0,0.72)", "color:#fff",
    "font:12px/1.4 ui-monospace,Menlo,monospace", "border-radius:6px",
    "pointer-events:none", "white-space:pre", "max-width:50vw"
  ].join(";");
  el.textContent = "waiting for data...";
  document.body.appendChild(el);
  let lastSource = "-";
  return {
    setSource(src) { lastSource = src; },
    update(imu) {
      const fmt = (n) => (typeof n === "number" ? n.toFixed(2) : "-");
      const rot = state?.joints || {};
      const imuLine = imu
        ? `IMU in   boom=${fmt(imu.boom)}  stick=${fmt(imu.stick)}  bucket=${fmt(imu.bucket)}`
        : `IMU in   (n/a)`;
      el.textContent =
        `source: ${lastSource}\n` +
        `${imuLine}\n` +
        `rotZ     boom=${fmt(rot.boom?.z)}  stick=${fmt(rot.stick?.z)}  bucket=${fmt(rot.bucket?.z)}\n` +
        `sign     boom=${IMU_CONFIG.boom.sign}  stick=${IMU_CONFIG.stick.sign}  bucket=${IMU_CONFIG.bucket.sign}`;
    }
  };
})();

function findByNameCaseInsensitive(root, name) {
  const nameLower = name.toLowerCase();
  let exact = null;
  let includes = null;

  root.traverse((obj) => {
    if (!obj.name) return;
    const objName = obj.name.toLowerCase();
    if (!exact && objName === nameLower) exact = obj;
    if (!includes && objName.includes(nameLower)) includes = obj;
  });

  return exact || includes;
}

function enableExcavatorCastShadows(root) {
  if (!root) return;
  root.traverse((obj) => {
    if (obj.isMesh) obj.castShadow = true;
  });
}

function tintMeshes(target, colorHex, metalness = 0.35, roughness = 0.7, opacity = 1) {
  if (!target) return;
  target.traverse((obj) => {
    if (!obj.isMesh) return;
    const sourceMat = Array.isArray(obj.material) ? obj.material[0] : obj.material;
    const nextMat = sourceMat ? sourceMat.clone() : new THREE.MeshStandardMaterial();
    nextMat.color = new THREE.Color(colorHex);
    if ("metalness" in nextMat) nextMat.metalness = metalness;
    if ("roughness" in nextMat) nextMat.roughness = roughness;
    nextMat.transparent = opacity < 1;
    nextMat.opacity = opacity;
    obj.material = nextMat;
  });
}

function applyExcavatorColors() {
  // Car body: lighter #80AAFF; arm segments: solid theme blue #004DFF (no along-arm fade).
  tintMeshes(nodes.main, partColors.themeBody, 0.25, 0.65, 1.0);
  tintMeshes(nodes.car, partColors.themeBody, 0.25, 0.65, 1.0);
  tintMeshes(nodes.driverCabin, partColors.themeBody, 0.25, 0.65, 1.0);
  tintMeshes(nodes.track, partColors.themeBody, 0.4, 0.6, 1.0);
  const armMat = [partColors.theme, 0.3, 0.7, 1.0];
  tintMeshes(nodes.base, ...armMat);
  tintMeshes(nodes.boom, ...armMat);
  tintMeshes(nodes.stick, ...armMat);
  tintMeshes(nodes.bucket, ...armMat);
  tintMeshes(nodes.diggingBucket, ...armMat);
}

function applyStateToModel() {
  if (!nodes.main) return;

  nodes.main.rotation.set(
    degToRad(state.main.pitch),
    degToRad(state.main.yaw),
    degToRad(state.main.roll)
  );

  ["base", "boom", "stick", "bucket"].forEach((jointName) => {
    const node = nodes[jointName];
    if (!node) return;
    const joint = state.joints[jointName];
    node.rotation.set(degToRad(joint.x), degToRad(joint.y), degToRad(joint.z));
  });

  ["boom", "stick"].forEach((jointName) => {
    const node = nodes[jointName];
    if (!node) return;
    const base = baseScale[jointName];
    const axis = lengthAxis[jointName];
    const lengthScale = Math.max(0.1, Number(state.lengths[jointName]) || 1);

    node.scale.set(base.x, base.y, base.z);
    if (axis === "x") node.scale.x = base.x * lengthScale;
    if (axis === "y") node.scale.y = base.y * lengthScale;
    if (axis === "z") node.scale.z = base.z * lengthScale;
  });
}

const loader = new GLTFLoader();
loader.load(
  "./model/excavator.glb",
  (gltf) => {
    const root = gltf.scene;
    scene.add(root);
    enableExcavatorCastShadows(root);

    nodes.main = findByNameCaseInsensitive(root, "main") || root;
    nodes.car = findByNameCaseInsensitive(root, "car");
    nodes.armature = findByNameCaseInsensitive(nodes.main, "armature");
    nodes.base = findByNameCaseInsensitive(root, "base");
    nodes.boom = findByNameCaseInsensitive(root, "boom");
    nodes.stick = findByNameCaseInsensitive(root, "stick");
    nodes.bucket = findByNameCaseInsensitive(root, "bucket");
    nodes.driverCabin = findByNameCaseInsensitive(root, "driver-cabin") || findByNameCaseInsensitive(root, "cabin");
    nodes.track = findByNameCaseInsensitive(root, "track");
    nodes.diggingBucket =
      findByNameCaseInsensitive(root, "digging-bucket") || findByNameCaseInsensitive(root, "front-bucket");

    console.log("Excavator nodes:", {
      main: nodes.main,
      car: nodes.car,
      armature: nodes.armature,
      base: nodes.base,
      boom: nodes.boom,
      stick: nodes.stick,
      bucket: nodes.bucket,
      driverCabin: nodes.driverCabin,
      track: nodes.track,
      diggingBucket: nodes.diggingBucket
    });

    if (nodes.boom) baseScale.boom.copy(nodes.boom.scale);
    if (nodes.stick) baseScale.stick.copy(nodes.stick.scale);
    applyExcavatorColors();

    const box = new THREE.Box3().setFromObject(root);
    const center = box.getCenter(new THREE.Vector3());
    const size = box.getSize(new THREE.Vector3());
    // Fit shadow frustum to model size so the shadow-map boundary stays offscreen.
    shadowExtent = Math.max(60, Math.max(size.x, size.z) * 3.0);
    sun.shadow.camera.left = -shadowExtent;
    sun.shadow.camera.right = shadowExtent;
    sun.shadow.camera.top = shadowExtent;
    sun.shadow.camera.bottom = -shadowExtent;
    sun.shadow.camera.far = Math.max(140, size.y * 12);
    sun.shadow.camera.updateProjectionMatrix();
    sun.shadow.needsUpdate = true;
    sun.target.position.copy(center);
    // Sun sits above the cab / upper body so light and shadow read as “overhead”.
    const sunOverhead = new THREE.Vector3(1.2, 14, 1.5);
    sun.position.copy(center).add(sunOverhead);
    if (controls) controls.target.copy(center);
    if (isDevPanelEnabled) camera.lookAt(center);

    applyStateToModel();
  },
  undefined,
  (error) => {
    console.error("Failed to load ./model/excavator.glb", error);
  }
);

const guiControllers = [];

function trackController(controller) {
  if (!controller) return null;
  guiControllers.push(controller);
  return controller;
}

function refreshGui() {
  guiControllers.forEach((controller) => controller.updateDisplay());
}

if (isDevPanelEnabled) {
  const gui = new GUI({ title: "Excavator Dev Panel" });
  const mainFolder = gui.addFolder("main (cab sway)");
  trackController(mainFolder.add(state.main, "roll", -45, 45, 0.1)).name("roll (left/right)").onChange(applyStateToModel);
  trackController(mainFolder.add(state.main, "pitch", -45, 45, 0.1)).name("pitch (front/back)").onChange(applyStateToModel);
  trackController(mainFolder.add(state.main, "yaw", -180, 180, 0.1)).name("yaw").onChange(applyStateToModel);

  const jointsFolder = gui.addFolder("armature joints");
  ["base", "boom", "stick", "bucket"].forEach((jointName) => {
    const folder = jointsFolder.addFolder(jointName);
    trackController(folder.add(state.joints[jointName], "x", -180, 180, 0.1)).name("rotX").onChange(applyStateToModel);
    trackController(folder.add(state.joints[jointName], "y", -180, 180, 0.1)).name("rotY").onChange(applyStateToModel);
    trackController(folder.add(state.joints[jointName], "z", -180, 180, 0.1)).name("rotZ").onChange(applyStateToModel);
  });

  const lengthsFolder = gui.addFolder("arm length scale (1 = 1m)");
  trackController(lengthsFolder.add(state.lengths, "boom", 0.5, 2.5, 0.01)).name("boom length").onChange(applyStateToModel);
  trackController(lengthsFolder.add(state.lengths, "stick", 0.5, 2.5, 0.01)).name("stick length").onChange(applyStateToModel);

  mainFolder.open();
  jointsFolder.open();
  lengthsFolder.open();
}

window.excavatorController = {
  state,
  apply: applyStateToModel,
  setMain(partialMain = {}) {
    Object.assign(state.main, partialMain);
    applyStateToModel();
    refreshGui();
  },
  setJoint(jointName, partialJoint = {}) {
    if (!state.joints[jointName]) return;
    // Arm joints (boom/stick/bucket) treat incoming .z as IMU reading,
    // so sign conversion is applied consistently with setImu / postMessage.
    const converted = { ...partialJoint };
    if ((jointName === "boom" || jointName === "stick" || jointName === "bucket") &&
        typeof converted.z === "number") {
      converted.z = imuToLocalAngle(jointName, converted.z);
    }
    Object.assign(state.joints[jointName], converted);
    debugOverlay.setSource(`setJoint(${jointName})`);
    debugOverlay.update();
    applyStateToModel();
    refreshGui();
  },
  setLengths(partialLengths = {}) {
    if (typeof partialLengths.boom === "number") state.lengths.boom = partialLengths.boom;
    if (typeof partialLengths.stick === "number") state.lengths.stick = partialLengths.stick;
    applyStateToModel();
    refreshGui();
  },
  setAll(nextState = {}) {
    if (nextState.main) Object.assign(state.main, nextState.main);
    if (nextState.lengths) {
      if (typeof nextState.lengths.boom === "number") state.lengths.boom = nextState.lengths.boom;
      if (typeof nextState.lengths.stick === "number") state.lengths.stick = nextState.lengths.stick;
    }
    if (nextState.joints) {
      Object.keys(nextState.joints).forEach((jointName) => {
        if (!state.joints[jointName]) return;
        const incoming = nextState.joints[jointName];
        const converted = { ...incoming };
        if ((jointName === "boom" || jointName === "stick" || jointName === "bucket") &&
            incoming && typeof incoming.z === "number") {
          converted.z = imuToLocalAngle(jointName, incoming.z);
        }
        Object.assign(state.joints[jointName], converted);
      });
    }
    debugOverlay.setSource("setAll");
    debugOverlay.update();
    applyStateToModel();
    refreshGui();
  },
  // Accept raw IMU readings (already local/relative angles per embedded protocol).
  // Usage: window.excavatorController.setImu({ boom: -60, stick: -175, bucket: 19.2 })
  setImu({ boom: imuBoom, stick: imuStick, bucket: imuBucket } = {}) {
    if (typeof imuBoom === "number")   state.joints.boom.z   = imuToLocalAngle("boom",   imuBoom);
    if (typeof imuStick === "number")  state.joints.stick.z  = imuToLocalAngle("stick",  imuStick);
    if (typeof imuBucket === "number") state.joints.bucket.z = imuToLocalAngle("bucket", imuBucket);
    debugOverlay.update({ boom: imuBoom, stick: imuStick, bucket: imuBucket });
    applyStateToModel();
    refreshGui();
  },
  // Runtime tuning, e.g.:
  //   setImuConfig({ boom: { sign: 1 } })
  //   setImuConfig({ stick: { offset: -30 } })
  setImuConfig(partial = {}) {
    Object.keys(partial).forEach((joint) => {
      if (!IMU_CONFIG[joint]) return;
      Object.assign(IMU_CONFIG[joint], partial[joint]);
    });
  }
};

function applyExternalPayload(payload) {
  if (!payload || typeof payload !== "object") return false;
  console.log(`[applyExternalPayload] received: ${JSON.stringify(payload, null, 2)}`);//"[applyExternalPayload] received:", payload

  // Explicit IMU payload: { imu: { boom, stick, bucket } }
  if (payload.imu) {
    debugOverlay.setSource("postMessage → imu");
    window.excavatorController.setImu(payload.imu);
    return true;
  }

  // Shorthand: top-level boom/stick/bucket are treated as IMU readings too.
  // Accepts both { boom: -60 } and { boom: { z: -60 } } styles.
  const hasShorthandArm =
    ("boom" in payload || "stick" in payload || "bucket" in payload) &&
    !("joints" in payload);
  if (hasShorthandArm) {
    const pickAngle = (v) => (typeof v === "number" ? v : v && typeof v.z === "number" ? v.z : undefined);
    debugOverlay.setSource("postMessage → shorthand");
    window.excavatorController.setImu({
      boom: pickAngle(payload.boom),
      stick: pickAngle(payload.stick),
      bucket: pickAngle(payload.bucket)
    });
    return true;
  }

  debugOverlay.setSource("postMessage → setAll");
  window.excavatorController.setAll(payload);
  return true;
}

// Android can call: window.applyExcavatorPayload({...}) directly.
window.applyExcavatorPayload = (payload) => applyExternalPayload(payload);

// Android can inject JS: window.dispatchEvent(new MessageEvent("message",{data: ...}))
window.addEventListener("message", (event) => {
  const data = event.data;
  if (typeof data === "string") {
    try {
      applyExternalPayload(JSON.parse(data));
    } catch {
      // Ignore non-JSON strings.
    }
    return;
  }
  applyExternalPayload(data);
});

function animate() {
  requestAnimationFrame(animate);
  if (controls) controls.update();
  renderer.render(scene, camera);
}
animate();

window.addEventListener("resize", () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
});
