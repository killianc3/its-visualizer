
# Intelligent Transport Systems Interaction Visualization Interface

## Overview

This project is a Java-based application developed to visualize the communication between autonomous vehicles and infrastructures within intelligent transport systems (ITS). The application was developed as part of an internship at the LABI*, which is involved in the European InDiD project. The goal of this project is to study ITS communication systems, their coverage, efficiency, and architecture for a future large-scale deployment.

## Usage
- Data Acquisition: Start the data acquisition module to capture or load existing data.
- Visualization: Use the playback controls to navigate through the data timeline.
- Filtering: Apply filters to focus on specific types of exchanges.
- Multi-Camera Views: Add multiple camera views to track different station groups.
- Synchronization: Adjust the timeline to synchronize station data if necessary

## Portability

The application has been tested and is compatible with:

- Java versions: 8 to 22
- JavaFX versions: All compatible with the Java versions
- Platforms: Windows, Linux, MacOS, and Raspberry Pi 4

## Installation Guide

- Clone the repository:
    ```bash
    git clone https://github.com/killianc3/its-visualizer
    cd its-visualizer
    ```

- Download JavaFX:
    - Download [JavaFX](https://gluonhq.com/products/javafx/) for your system.
    - Place the downloaded JavaFX files in the `lib` folder within the project directory.

- Run the application:
    - On Windows:
      ```bash
      .\run.ps1
      ```
    - On MacOS/Linux:
      ```bash
      ./run.sh
      ```