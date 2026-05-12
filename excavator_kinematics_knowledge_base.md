# 挖掘机 IMU 正运动学知识库（Excavator Kinematics Knowledge Base）

## 1. 项目目标

利用以下传感器数据实时计算挖掘机斗尖（Bucket Tooth）的空间位置：

- 三个 IMU（大臂、小臂、铲斗）
- 驾驶室 IMU（Pitch / Roll）
- GNSS + RTK（经纬度 + Yaw）

输出能力包括：

- 斗尖三维坐标 `(x, y, z)`
- 左右斗齿坐标
- 斗尖经纬度
- 铲斗与地面的距离
- 填挖量（Cut / Fill）
- 找平 / 挖渠 / 修坡辅助

---

## 2. 系统结构

```text
Base (车体)
 └── Boom (大臂)
      └── Stick (小臂)
           └── Bucket (铲斗)
                └── Tooth (斗尖)
```

这是一个典型的串联机械臂问题，可通过正运动学求解。

---

## 3. 输入数据

### 3.1 实时数据

#### 大臂 IMU
- `boom_abs_pitch_deg`

#### 小臂 IMU
- `stick_abs_pitch_deg`

#### 铲斗 IMU
- `bucket_link_abs_pitch_deg`

#### 驾驶室 IMU
- `cab_pitch_deg`
- `cab_roll_deg`

#### GNSS / RTK
- `yaw_deg`
- `latitude`
- `longitude`
- `altitude`

### 3.2 标定参数

```json
{
  "boom_length": 4.5,
  "stick_length": 2.6,
  "bucket_tip_length": 1.2,
  "bucket_width": 1.1,

  "boom_install_offset_deg": 0.0,
  "stick_install_offset_deg": 0.0,
  "bucket_install_offset_deg": 0.0
}
```

---

## 4. 绝对角与相对角

IMU 返回的是相对水平面的绝对俯仰角。

### 4.1 修正后的绝对角

```text
boom_abs   = boom_imu   + boom_install_offset
stick_abs  = stick_imu  + stick_install_offset
bucket_abs = bucket_imu + bucket_install_offset
```

### 4.2 相对关节角

```text
θ1 = boom_abs
θ2 = stick_abs  - boom_abs
θ3 = bucket_abs - stick_abs
```

---

## 5. 二维正运动学

```math
x = L1 cos(θ1)
  + L2 cos(θ1 + θ2)
  + L3 cos(θ1 + θ2 + θ3)

z = L1 sin(θ1)
  + L2 sin(θ1 + θ2)
  + L3 sin(θ1 + θ2 + θ3)
```

其中：

- `L1` = 大臂长度
- `L2` = 小臂长度
- `L3` = 斗尖等效长度

---

## 6. 三维坐标

GNSS 提供整机偏航角 `yaw`。

```text
X_global = x * cos(yaw)
Y_global = x * sin(yaw)
Z_global = z
```

---

## 7. 左右斗齿

```text
left_tooth  = (x, -bucket_width / 2, z)
right_tooth = (x,  bucket_width / 2, z)
```

---

## 8. 与地面距离

若目标地面高度为：

```text
ground_z
```

则：

```text
distance_to_ground = z_tip - ground_z
```

解释：

- `> 0`：斗尖高于地面
- `= 0`：斗尖接触地面
- `< 0`：斗尖低于目标面（超挖）

---

## 9. 填挖量（Cut / Fill）

```text
cut_fill = z_tip - design_surface_z
```

- 正值：高于设计面，需要继续挖
- 负值：低于设计面，已经超挖

---

## 10. 设计面类型

### 10.1 水平面
```text
z = constant
```

### 10.2 坡面
```text
ax + by + cz + d = 0
```

### 10.3 沟槽底面
沿中心线定义宽度和深度。

---

## 11. 经纬度转换

可将局部 ENU 坐标转换为全球坐标：

- East → 经度偏移
- North → 纬度偏移
- Up → 海拔

---

## 12. 推荐数据结构

```json
{
  "jointAngles": {
    "boom": 32.1,
    "stick": -47.8,
    "bucket": 15.4
  },
  "tipPosition": {
    "x": 5.23,
    "y": 0.14,
    "z": 1.87
  },
  "distanceToGround": 0.12,
  "cutFill": -0.08
}
```

---

## 13. TypeScript 接口

```ts
interface SensorInput {
  boomAbsPitchDeg: number;
  stickAbsPitchDeg: number;
  bucketAbsPitchDeg: number;
  yawDeg: number;
}

interface Calibration {
  boomLength: number;
  stickLength: number;
  bucketTipLength: number;
  bucketWidth: number;

  boomInstallOffsetDeg: number;
  stickInstallOffsetDeg: number;
  bucketInstallOffsetDeg: number;
}

interface Vec3 {
  x: number;
  y: number;
  z: number;
}

interface KinematicsResult {
  tip: Vec3;
  leftTooth: Vec3;
  rightTooth: Vec3;
  distanceToGround: number;
  cutFill: number;
}
```

---

## 14. 核心算法流程

```text
读取传感器
   ↓
安装角修正
   ↓
绝对角 → 相对角
   ↓
正运动学
   ↓
斗尖 XYZ
   ↓
参考面求距离
   ↓
输出 UI
```

---

## 15. TypeScript 伪代码

```ts
function solve(input, calib, groundZ): KinematicsResult {
  const boomAbs =
    input.boomAbsPitchDeg + calib.boomInstallOffsetDeg;
  const stickAbs =
    input.stickAbsPitchDeg + calib.stickInstallOffsetDeg;
  const bucketAbs =
    input.bucketAbsPitchDeg + calib.bucketInstallOffsetDeg;

  const t1 = deg2rad(boomAbs);
  const t2 = deg2rad(stickAbs - boomAbs);
  const t3 = deg2rad(bucketAbs - stickAbs);

  const x =
    calib.boomLength * Math.cos(t1) +
    calib.stickLength * Math.cos(t1 + t2) +
    calib.bucketTipLength * Math.cos(t1 + t2 + t3);

  const z =
    calib.boomLength * Math.sin(t1) +
    calib.stickLength * Math.sin(t1 + t2) +
    calib.bucketTipLength * Math.sin(t1 + t2 + t3);

  const tip = { x, y: 0, z };

  return {
    tip,
    leftTooth: {
      x,
      y: -calib.bucketWidth / 2,
      z
    },
    rightTooth: {
      x,
      y: calib.bucketWidth / 2,
      z
    },
    distanceToGround: z - groundZ,
    cutFill: z - groundZ
  };
}
```

---

## 16. 精度估算

若：

- 总长度 = 8 m
- IMU 误差 = 0.1°

则：

```math
Δh ≈ 8 × sin(0.1°) ≈ 1.4 cm
```

---

## 17. 常见误差来源

1. IMU 安装角误差
2. 连杆长度测量误差
3. 机械间隙
4. GNSS 漂移
5. 传感器噪声

---

## 18. 标定流程

### 零位标定

将设备置于已知姿态，记录各 IMU 偏移量。

### 尺寸标定

测量：

- 大臂长度
- 小臂长度
- 斗尖长度
- 斗宽

