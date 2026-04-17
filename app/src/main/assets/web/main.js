import * as THREE from "three";
import { GLTFLoader } from "three/addons/loaders/GLTFLoader.js";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";
import { GUI } from "three/addons/libs/lil-gui.module.min.js";

const app = document.getElementById("app");
const isDevPanelEnabled = new URLSearchParams(window.location.search).get("dev") === "1";

const scene = new THREE.Scene();
scene.background = null;

const camera = new THREE.PerspectiveCamera(50, window.innerWidth / window.innerHeight, 0.1, 200);
camera.position.set(6, 3.5, 7);

const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setClearColor(0x000000, 0);
app.appendChild(renderer.domElement);

const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.target.set(0, 1.2, 0);

scene.add(new THREE.HemisphereLight(0xffffff, 0x334455, 1.2));
const sun = new THREE.DirectionalLight(0xffffff, 1.2);
sun.position.set(8, 12, 6);
sun.castShadow = true;
scene.add(sun);

// Thin reference plane (always on): light gray so roll/pitch reads clearly vs transparent embed.
const groundSize = 40;
const groundGeo = new THREE.PlaneGeometry(groundSize, groundSize);
const groundMat = new THREE.MeshStandardMaterial({
  color: 0xeeeeee,
  roughness: 0.92,
  metalness: 0
});
const ground = new THREE.Mesh(groundGeo, groundMat);
ground.rotation.x = -Math.PI / 2;
ground.position.y = -0.02;
ground.receiveShadow = true;
scene.add(ground);

if (isDevPanelEnabled) {
  const grid = new THREE.GridHelper(20, 20, 0x888888, 0x444444);
  grid.position.y = -0.001;
  scene.add(grid);

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
  bucket: { sign: -1 }
};

function imuToLocalAngle(jointName, imuValue) {
  const cfg = IMU_CONFIG[jointName];
  if (!cfg) return imuValue;
  return cfg.sign * imuValue;
}

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
  // Car body (node: "car"): lighter/whiter #80AAFF, fully opaque.
  // Armature arms: deeper #004DFF, opacity fades outward from base (80%) to bucket (50%).
  tintMeshes(nodes.main, partColors.themeBody, 0.25, 0.65, 1.0);
  tintMeshes(nodes.car, partColors.themeBody, 0.25, 0.65, 1.0);
  tintMeshes(nodes.driverCabin, partColors.themeBody, 0.25, 0.65, 1.0);
  tintMeshes(nodes.track, partColors.themeBody, 0.4, 0.6, 1.0);
  tintMeshes(nodes.base, partColors.theme, 0.3, 0.7, 0.8);
  tintMeshes(nodes.boom, partColors.theme, 0.3, 0.7, 0.7);
  tintMeshes(nodes.stick, partColors.theme, 0.3, 0.7, 0.6);
  tintMeshes(nodes.bucket, partColors.theme, 0.3, 0.7, 0.5);
  tintMeshes(nodes.diggingBucket, partColors.theme, 0.3, 0.7, 0.5);
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
    controls.target.copy(center);
    camera.lookAt(center);

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
    Object.assign(state.joints[jointName], partialJoint);
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
        Object.assign(state.joints[jointName], nextState.joints[jointName]);
      });
    }
    applyStateToModel();
    refreshGui();
  },
  // Accept raw IMU readings (already local/relative angles per embedded protocol).
  // Usage: window.excavatorController.setImu({ boom: -60, stick: -175, bucket: 19.2 })
  setImu({ boom: imuBoom, stick: imuStick, bucket: imuBucket } = {}) {
    if (typeof imuBoom === "number")   state.joints.boom.z   = imuToLocalAngle("boom",   imuBoom);
    if (typeof imuStick === "number")  state.joints.stick.z  = imuToLocalAngle("stick",  imuStick);
    if (typeof imuBucket === "number") state.joints.bucket.z = imuToLocalAngle("bucket", imuBucket);
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
  // IMU payload: { imu: { boom, stick, bucket } }
  if (payload.imu) {
    window.excavatorController.setImu(payload.imu);
    return true;
  }
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
  controls.update();
  renderer.render(scene, camera);
}
animate();

window.addEventListener("resize", () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
});
