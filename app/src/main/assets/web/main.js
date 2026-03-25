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

const LENGTHS_STORAGE_KEY = "excavator_arm_lengths";

function loadLengthsFromLocalStorage() {
  try {
    const raw = localStorage.getItem(LENGTHS_STORAGE_KEY);
    if (!raw) return;
    const parsed = JSON.parse(raw);
    if (typeof parsed.boom === "number" && Number.isFinite(parsed.boom)) {
      state.lengths.boom = Math.max(0.1, parsed.boom);
    }
    if (typeof parsed.stick === "number" && Number.isFinite(parsed.stick)) {
      state.lengths.stick = Math.max(0.1, parsed.stick);
    }
  } catch {
    // ignore
  }
}

function persistLengthsToLocalStorage() {
  try {
    localStorage.setItem(
      LENGTHS_STORAGE_KEY,
      JSON.stringify({ boom: state.lengths.boom, stick: state.lengths.stick })
    );
  } catch {
    // ignore (e.g. private mode)
  }
}

loadLengthsFromLocalStorage();

const nodes = {
  main: null,
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
  bodyYellow: 0xffc107,
  brightYellow: 0xffd54f,
  accentOrange: 0xffa000,
  black: 0x1c1f24
};

function degToRad(value) {
  return THREE.MathUtils.degToRad(value);
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

function tintMeshes(target, colorHex, metalness = 0.35, roughness = 0.7) {
  if (!target) return;
  target.traverse((obj) => {
    if (!obj.isMesh) return;
    const sourceMat = Array.isArray(obj.material) ? obj.material[0] : obj.material;
    const nextMat = sourceMat ? sourceMat.clone() : new THREE.MeshStandardMaterial();
    nextMat.color = new THREE.Color(colorHex);
    if ("metalness" in nextMat) nextMat.metalness = metalness;
    if ("roughness" in nextMat) nextMat.roughness = roughness;
    obj.material = nextMat;
  });
}

function applyExcavatorColors() {
  // High-visibility scheme: mostly engineering yellow, with dark tracks for contrast.
  tintMeshes(nodes.main, partColors.bodyYellow, 0.32, 0.72);
  tintMeshes(nodes.base, partColors.brightYellow, 0.3, 0.74);
  tintMeshes(nodes.boom, partColors.brightYellow, 0.3, 0.74);
  tintMeshes(nodes.stick, partColors.bodyYellow, 0.32, 0.72);
  tintMeshes(nodes.bucket, partColors.accentOrange, 0.4, 0.6);

  tintMeshes(nodes.driverCabin, partColors.bodyYellow, 0.32, 0.72);
  tintMeshes(nodes.track, partColors.black, 0.5, 0.58);
  tintMeshes(nodes.diggingBucket, partColors.accentOrange, 0.4, 0.6);
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
  }
};

function applyExternalPayload(payload) {
  if (!payload || typeof payload !== "object") return false;
  window.excavatorController.setAll(payload);
  if (
    payload.lengths &&
    (typeof payload.lengths.boom === "number" || typeof payload.lengths.stick === "number")
  ) {
    persistLengthsToLocalStorage();
  }
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
