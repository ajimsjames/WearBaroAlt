# 🎈 WearBaroAlt (v1.0.0)

[![Wear OS](https://img.shields.io/badge/Wear%20OS-5.0-blue.svg)](https://developer.android.com/wear)
[![Device](https://img.shields.io/badge/Target-Samsung%20Galaxy%20Watch%206-black.svg)](https://www.samsung.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**WearBaroAlt** is a standalone hardware barometric pressure weather station, QNH sea-level altimeter, and rapid storm drop detector built specifically for **Samsung Wear OS Smartwatches** (optimized for **Galaxy Watch 6**). It interfaces with the hardware **LPS28DFW Barometer** to log pressure trends and alert users to approaching storm/rain systems.

Developed by **Aju George** ([@ajimsjames](https://github.com/ajimsjames)).

---

## ✨ Features

- 🎈 **LPS28DFW Barometer Station**: Real-time atmospheric pressure readout in `hPa`, `mbar`, and `inHg`.
- 📉 **Pressure Trend Graph**: Live sparkline graph tracking pressure fluctuations over time.
- ⛰️ **QNH Sea-Level Altimeter**: Elevation calculation in meters (`m`) & feet (`ft`).
- ⛈️ **Storm Drop Detector**: Alerts user when barometric pressure drops rapidly (`> 2.5 hPa`), indicating approaching rain/storm.
- ⭕ **Samsung One UI Bezel Navigation**: Curved top navigation bar (`CurvedLayout`) with 40.dp top clearance.

---

## 🛠️ Technology Stack

* **Platform**: Android Wear OS (Min SDK 30 / Target SDK 33 / Wear OS 5)
* **Language**: Kotlin 1.9
* **Hardware Sensors**: LPS28DFW Barometer (`Sensor.TYPE_PRESSURE`)
* **UI Framework**: Jetpack Compose for Wear OS + Custom Sparkline Canvas

---

## 🚀 Installation via Wireless ADB

```bash
adb connect <WATCH_IP_ADDRESS>:<PORT>
adb install -r WearBaroAlt-v1.0.0.apk
```

---

## 👨‍💻 Author

Developed by **Aju George**  
GitHub: [@ajimsjames](https://github.com/ajimsjames)
