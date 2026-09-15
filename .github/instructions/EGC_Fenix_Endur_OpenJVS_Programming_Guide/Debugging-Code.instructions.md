---
description: "Use when explaining, implementing, or reviewing how to debug OpenJVS Java plug-ins in Endur using the Eclipse IDE: initiating debug sessions, initiating a JVM on a script engine, setting breakpoints, the Debug/Variable/Breakpoint/Expression views, displaying memory table contents, and stepping through code. Trigger keywords: Debugging Code, Debug Remote Project, Debug Local Project, Script Engine, Trading Manager JVM, breakpoint, Debug View, Display view, viewTable, step into, step over."
name: "Debugging Code"
applyTo: "**/*.java"
---

# Debugging Code — Full Reference Guide

> Source: Section 11 *"Debugging Code"* of the **Programming Guide for OpenJVS Development — EET Fenix Endur**, covering 11.1 Initiating a Debug Session, 11.2 Initiating a JVM for Debugging, 11.3 Setting Breakpoints, and 11.4 Starting a Debug Session (11.4.1 Debug View, 11.4.2 Variable/Breakpoint/Expression Views, 11.4.3 Displaying Contents of Memory Tables, 11.4.4 Stepping Through Code). This instruction file expands the section into a complete, diagrammed reference for debugging OpenJVS plug-ins running inside Endur.

---

## Table of Contents

1. [Overview: Debugging Native Java Running Inside Endur](#1-overview-debugging-native-java-running-inside-endur)
2. [11.1 Initiating a Debug Session](#2-111-initiating-a-debug-session)
3. [11.2 Initiating a JVM for Debugging](#3-112-initiating-a-jvm-for-debugging)
4. [11.3 Setting Breakpoints](#4-113-setting-breakpoints)
5. [11.4 Starting a Debug Session](#5-114-starting-a-debug-session)
   - 11.4.1 [Debug View](#511-debug-view)
   - 11.4.2 [Variable/Breakpoint/Expression Views](#512-variablebreakpointexpression-views)
   - 11.4.3 [Displaying Contents of Memory Tables](#513-displaying-contents-of-memory-tables)
   - 11.4.4 [Stepping Through Code](#514-stepping-through-code)
6. [End-to-End Debug Session Walkthrough](#6-end-to-end-debug-session-walkthrough)
7. [Practical Checklist for Developers](#7-practical-checklist-for-developers)

---

## 1. Overview: Debugging Native Java Running Inside Endur

> The **Eclipse IDE has a very powerful debugger** which can be used for debugging OpenJVS classes. However in order to use the debugger the **Eclipse IDE session must be associated with an Endur session**. For this, Eclipse needs to be **launched from a running Endur instance**. In the Fenix environments an Endur task has been created named **`Eclipse IDE`** which launches Eclipse from Endur and associates it with the currently open Endur session.

This precondition is identical to the one governing plugin import in Section 10.3 — debugging and importing both require Eclipse to be a "child" of a live Endur session, not a standalone Eclipse instance.

```mermaid
graph TD
    Endur["Running Endur instance"] --> Task["Run the 'Eclipse IDE' task"]
    Task --> Eclipse["Eclipse launches,<br/>associated with THIS Endur session"]
    Eclipse --> Ready["✅ Debug Remote/Local Project<br/>options now available"]

    Standalone["Eclipse opened normally"] -.-> NotReady["❌ No debug connection<br/>to any Endur JVM possible"]

    style Ready fill:#c9f7c5
    style NotReady fill:#fbe0e0
```

### The Four-Stage Debugging Journey

```mermaid
flowchart LR
    S1["11.1 Initiate a<br/>Debug Session<br/>(connect Eclipse to<br/>an Endur JVM)"] --> S2["11.2 Initiate a JVM<br/>(only if none exists yet)"]
    S2 --> S3["11.3 Set Breakpoints<br/>(in Eclipse source)"]
    S3 --> S4["11.4 Start/Run the<br/>Debug Session<br/>(execute the script,<br/>hit the breakpoint,<br/>inspect state)"]
```

---

## 2. 11.1 Initiating a Debug Session

### Purpose

> The **OpenLink OpenJVS Eclipse plugin** is used to launch a debugging session and connect it to an **Endur Java JVM instance**.

### Step-by-Step Process

| Step | Action |
|---|---|
| 1 | Right-click an **Eclipse OpenJVS project** and select **`OpenLink -> Debug Remote Project`**. *(Note: if `Debug Remote Project` does not work, use `Debug Local Project` instead.)* |
| 2 | A window displays the **available sessions** to connect to. Choose a session and click **`OK`**. You *can* choose a session running on an **app server** to debug OpenJVS code running there, but **normally you choose your own session**. |
| 3 | Another window displays the **JVMs available to connect to**. If debugging a **parameter script**, choose **`Trading Manager`**; otherwise choose **`Script Engine x`**, then click `OK`. If **no JVMs are available**, one must be **manually started** (see Section 3 below). |

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Eclipse as Eclipse (OpenLink menu)
    participant SessionList as Select User Session window
    participant JvmList as Select JVM to attach window
    participant JVM as Endur JVM (Script Engine / Trading Manager)

    Dev->>Eclipse: Right-click project →<br/>OpenLink → Debug Remote Project
    Eclipse->>SessionList: Show available Endur sessions<br/>(OLF Process ID, Session ID, User Name,<br/>Login Name, Host Name)
    Dev->>SessionList: Select own session (usually), click OK
    SessionList->>JvmList: Show JVMs available on that session<br/>(Master, OLF Presentation Module,<br/>Script Engine 1..N, Trading Manager)
    Dev->>JvmList: Choose JVM based on script type:<br/>Parameter script → Trading Manager<br/>Other → Script Engine x
    Dev->>JvmList: Click OK
    JvmList->>JVM: Attach Eclipse debugger
    JVM-->>Dev: ✅ Debug session connected
```

### Which JVM Should You Choose?

| Plug-in Type Being Debugged | Choose |
|---|---|
| **Parameter script** (extends `BasicParamScript`) | **Trading Manager** |
| **Any other plug-in type** (Main, Output, UDSR, OpServices, Connex) | **Script Engine x** |

```mermaid
flowchart TD
    Q{"What type of script<br/>are you debugging?"}
    Q -- "Parameter Plug-in<br/>(extends BasicParamScript)" --> TM["Attach to: Trading Manager"]
    Q -- "Main / Output / UDSR /<br/>OpServices / Connex" --> SE["Attach to: Script Engine x"]
```

### Selecting the Right Session

The **Select User Session** window lists columns: **OLF Process ID**, **Session ID**, **User Name**, **Login Name**, **Host Name**. In practice:

- Choose **your own session** (matching your login name) for standard development debugging.
- Choose a **different (e.g., app server) session** only when you specifically need to debug OpenJVS code executing on that server rather than your own client session.

---

## 3. 11.2 Initiating a JVM for Debugging

### Why This Step Is Sometimes Needed

> It may be necessary to **manually initiate a JVM** on a script engine if one does not already exist. Generally a JVM will be started **if an OpenJVS script has already been executed** on a script engine.

### Two Ways to Get a JVM Running

```mermaid
flowchart TD
    Need(["Need a JVM to attach to,<br/>but 'Select JVM to attach' shows<br/>'JVM Not Initialized'"]) --> Q{"Preferred approach?"}
    Q -- "Indirect / natural" --> RunTask["Load and execute a task<br/>that runs an OpenJVS script<br/>→ JVM starts automatically"]
    Q -- "Direct / explicit" --> SysMon["Use the System Monitor<br/>(steps below)"]
    RunTask --> Restart["Restart the debug session<br/>(Section 11.1) — JVM now available"]
    SysMon --> Restart
```

### Manual Initiation via System Monitor

| Step | Action |
|---|---|
| 1 | Start the **System Monitor** (from Endur's `OpenLink` toolbar). |
| 2 | In the **bottom pane**, highlight the **script engine** to start the JVM on, then choose **`Debug -> Init Java VM`** from the menu bar. |
| 3 | **Close** the System Monitor and **restart the debug session** as described in Section 11.1. |

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant SysMon as System Monitor
    participant Engine as Script Engine (e.g. Engine #1)

    Dev->>SysMon: Start System Monitor
    Dev->>SysMon: Select script engine in bottom pane
    Dev->>SysMon: Menu: Debug → Init Java VM
    SysMon->>Engine: Initialize JVM on this engine
    Engine-->>SysMon: JVM now running (was "Not Initialized")
    Dev->>SysMon: Close System Monitor
    Dev->>Dev: Restart debug session (Section 11.1)<br/>— JVM now appears in "Select JVM to attach"
```

### System Monitor Overview (Context)

The System Monitor's **Script Engines** tab shows a **Jobs** panel (Job ID/Task Name, Script, Job Status, Posted Status, Submitter, Engine Number, Repeat Interval) and an **Engines** panel (Engine #, Status, PID, Job ID/Task Name, Mem Size, Virtual Mem Size, Time of Status) with controls: **New Engine**, **Stop Engine**, **Stop All Engines**, and a **Number of Engines** field.

---

## 4. 11.3 Setting Breakpoints

### The Critical Precondition

> Breakpoints are set using the **source code within the Eclipse IDE project**. Therefore for correct debugging it is **essential that the code being debugged is identical** between what has been imported into Endur and the java source file in Eclipse.

```mermaid
flowchart TD
    Mismatch{"Is the Eclipse source<br/>file IDENTICAL to what<br/>was imported into Endur?"}
    Mismatch -- "✅ Yes" --> Works["Breakpoints hit exactly where expected<br/>— line numbers match, variables match"]
    Mismatch -- "❌ No (e.g. imported an<br/>older/different version)" --> Broken["⚠️ Breakpoints may not hit,<br/>hit at the wrong line,<br/>or show mismatched variable state"]

    style Works fill:#c9f7c5
    style Broken fill:#fbe0e0
```

This is exactly why Section 10 ("Importing Code to Endur") matters so much for debugging: whichever import method was used (Simple Editor, Bulk Import, or the Eclipse `Save Plugin to DB`), the class imported into Endur **must match** the Eclipse source you're setting breakpoints in.

### How to Set a Breakpoint

1. Open the **java source file** in the Eclipse editor.
2. **Double-click** within the **left margin** at the line where the breakpoint is to be set.

### Toggling Breakpoints via Right-Click

A **right-click** in the left margin shows a pop-up menu with:

| Menu Item | Purpose |
|---|---|
| **Toggle Breakpoint** | Add/remove a breakpoint at this line |
| **Disable Breakpoint** | Keep the breakpoint defined but inactive |
| **Go to Annotation** | Navigate to the annotation at this line |
| **Add Bookmark...** | Bookmark this line (unrelated to debugging) |
| **Add Task...** | Create an Eclipse task marker |
| **Show Quick Diff** | Toggle quick-diff gutter markers |
| **Show Line Numbers** | Toggle line number display |
| **Folding** | Code folding submenu |
| **Preferences...** | Editor preferences |
| **Breakpoint Properties...** | Configure conditional/hit-count breakpoint rules |

```mermaid
graph LR
    Margin["Left margin of<br/>Eclipse editor"] -->|Double-click| Toggle["Breakpoint set/removed<br/>(blue dot marker appears)"]
    Margin -->|Right-click| Menu["Popup menu:<br/>Toggle / Disable /<br/>Breakpoint Properties..."]
    Menu --> Properties["Breakpoint Properties:<br/>conditional breakpoints,<br/>hit-count breakpoints"]
```

---

## 5. 11.4 Starting a Debug Session

### How a Debug Session Actually Starts

> Once a debug session has been **initiated** (Section 11.1), a debug session can be **started** by simply **running the OpenJVS script** — usually by loading and executing an appropriate task from the **`Trading Manager`**. There are other ways of running tasks, and anyone familiar with Endur will know how.

> When the OpenJVS script is executed and the breakpoint reached, **control will be passed to the Eclipse debug session**. A window will display asking to **switch the perspective**.

```mermaid
sequenceDiagram
    participant User as Endur User
    participant Endur
    participant Script as OpenJVS Script (breakpoint set)
    participant Eclipse

    User->>Endur: Load & execute a task from Trading Manager<br/>(or another run method)
    Endur->>Script: Script begins execution
    Script->>Script: Execution reaches the breakpointed line
    Script-->>Eclipse: Control passed to Eclipse debugger
    Eclipse->>User: "Confirm Perspective Switch" dialog:<br/>"This kind of launch is configured to<br/>open the Debug perspective when it suspends.<br/>Do you want to open this perspective now?"
    User->>Eclipse: Click 'Yes'
    Eclipse->>User: Eclipse Debug perspective opens
```

Clicking **`Yes`** opens the **Eclipse Debug perspective**, which arranges several sub-windows explained in the following four subsections.

### 5.1 Debug View

> The **'Debug View'** shows the **current threads that are running**. It will be located at the **top left**. One of these threads will be in a **suspended state** — this is the thread that contains the current breakpoint. The thread shows the **java class stack** that has led to the breakpoint. Clicking on the class at the top of the stack will show the **source file at the current breakpoint**.

```mermaid
graph TD
    subgraph DebugView["Debug View (top-left)"]
        LaunchConfig["ant-openjvs-launcher [Ant Build]"]
        RemoteDebug["remote-debug-openjvs-launcher [Remote Java Application]"]
        JVMThread["Java HotSpot(TM) Server VM"]
        MainThread["Thread [main] (Suspended — breakpoint hit)"]
        Frame1["YourClass.execute(Table, Table) line: 39"]
        Frame2["YourClass(BasicScript).execute(IContainerContext) line: 43"]
        Frame3["JvsHandler.process(long, Object) line: 57"]
        Frame4["JvsController.executeJvs(...) line: 47"]
    end
    RemoteDebug --> JVMThread
    JVMThread --> MainThread
    MainThread --> Frame1
    Frame1 --> Frame2
    Frame2 --> Frame3
    Frame3 --> Frame4
```

**Reading the call stack (bottom → top):** `JvsController.executeJvs(...)` (Endur's native entry point) called `JvsHandler.process(...)`, which invoked `BasicScript.execute(IContainerContext)` (the `final` wrapper method — see the Common Framework instruction file), which delegated to **your class's** `execute(Table, Table)` — exactly where the breakpoint is currently suspended.

### 5.2 Variable/Breakpoint/Expression Views

> These views are shown in a **tabbed format** and allow **variable inspection, breakpoint management, and the evaluation of expressions** on variables. The views are shown in the **top right**.

#### The Variable View

> Allows the **inspection of variables** and can be used in some cases to **change the value of variables** for **'what if' testing**.

```mermaid
graph TD
    subgraph VariableView["Variable View example"]
        This["this : YourClassMain instance"]
        Argt["argt : Table (id=26)"]
        Returnt["returnt : Table (id=21)"]
        JvsCPtr["jvsCPtr : 509120568"]
        JvsOwnPtr["jvsOwnPtr : false"]
        Today["today : 40597"]
    end
    Returnt --> JvsCPtr
    Returnt --> JvsOwnPtr
```

Note that `Table` objects display their **native `IntPtr` fields** (`jvsCPtr`, `jvsOwnPtr`) — a direct visual confirmation of the OpenJVS Architecture concept that `Table` inherits `IntPtr` and holds a native memory handle (see the OpenJVS Architecture instruction file).

#### The Breakpoints View

> Allows **management of breakpoints** including **removal of breakpoints, setting conditional breakpoints and hit count breakpoints**. **Right click** on a listed breakpoint to show the options.

```mermaid
graph TD
    subgraph BreakpointsView["Breakpoints View example"]
        BP1["AbstractUdsr [line: 118] - dwExtract(ContextHelper)"]
        BP2["DbPurgeMarketRiskNordicProfile [line: 50] - purgeRiskView(int)"]
        BP3["YourClassMain [line: 39] - execute(Table, Table)"]
        BP4["UdsrTranGptDeltaByLegHourly [line: 100] - calculate(ContextHelper)"]
        BP5["UdsrTranGptDeltaByLegHourly [line: 350] - processCustomDeals(Table, int, int)"]
    end
```

Each entry can be **individually enabled/disabled** (checkbox), and right-clicking exposes **Breakpoint Properties** for conditional expressions (e.g., "only stop if `dealNum == 12345`") or hit-count triggers (e.g., "only stop on the 5th hit").

#### The Expression View

> Allows **expressions to be entered and evaluated**. Expressions can be **anything that can be actioned on a current in-scope class or variable**, such as getting the length of a string.

```mermaid
graph LR
    Expr["Expression View"] --> Ex1["\"sql.length()\" → 223"]
    Expr --> Ex2["Add new expression..."]
```

**Example use case:** while suspended inside a purging worker with a `StringBuffer sql` variable in scope, typing `sql.length()` into the Expression View evaluates it live against the current suspended state.

### 5.3 Displaying Contents of Memory Tables

> Those familiar with AVS scripts realise how useful it can be to **show the contents of memory tables** during debug sessions. To achieve this when debugging OpenJVS code, use the **`Display`** view. Add an expression that will evaluate an **in-scope `Table` object's `viewTable()` method**.

> Once the expression has been entered, **highlight the whole expression** and type **`Control+Shift+I`**. This will result in the **table being displayed**.

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant DisplayView as Display View (Eclipse)
    participant Table as In-scope Table object
    participant Window as Endur Table Viewer Window

    Dev->>DisplayView: Type expression: powerIndex.viewTable()
    Dev->>DisplayView: Highlight the whole expression
    Dev->>DisplayView: Press Ctrl+Shift+I
    DisplayView->>Table: Evaluate viewTable() on the<br/>currently suspended Table instance
    Table->>Window: Open a live grid view of<br/>the table's rows/columns
    Window-->>Dev: See columns (e.g. index_id, ref_source,<br/>adjustment_id, power) and row data,<br/>exactly as if opened natively in Endur
```

```mermaid
flowchart TD
    Step1["1. Suspend at a breakpoint<br/>where a Table variable is in scope"] --> Step2["2. Open the Display view tab"]
    Step2 --> Step3["3. Type: &lt;tableVariable&gt;.viewTable()"]
    Step3 --> Step4["4. Select/highlight the entire expression"]
    Step4 --> Step5["5. Press Ctrl+Shift+I"]
    Step5 --> Step6["6. A native Endur table grid window opens<br/>showing the live in-memory data"]
```

**Why this matters:** this is the single most powerful inspection technique for OpenJVS debugging — it lets a developer see the **actual row/column data** of any `Table` object at the exact moment of suspension, rather than just an opaque object reference.

### 5.4 Stepping Through Code

> To step through code during a debug session, use the **toolbar displayed in the 'Debug' view**.

| Icon Action | Meaning |
|---|---|
| **Continue execution** | Resume running until the next breakpoint or program end |
| **Pause/suspend execution** | Manually halt a running thread |
| **Terminate debug session** | Stop debugging entirely |
| **Step into code** | Enter the method being called on the current line |
| **Step over code** | Execute the current line without entering called methods |
| **Step return from current function** | Run until the current method returns to its caller |

```mermaid
flowchart TD
    Suspended(["Suspended at breakpoint"]) --> Continue["▶ Continue —<br/>run to next breakpoint"]
    Suspended --> Pause["⏸ Pause —<br/>manually suspend a running thread"]
    Suspended --> Terminate["⏹ Terminate —<br/>stop the debug session"]
    Suspended --> StepInto["⬇ Step Into —<br/>enter the called method"]
    Suspended --> StepOver["↷ Step Over —<br/>execute line, stay at this level"]
    Suspended --> StepReturn["↰ Step Return —<br/>run until current method returns"]

    StepInto --> Suspended
    StepOver --> Suspended
    StepReturn --> Suspended
```

```mermaid
graph LR
    A["Line N:<br/>result = helper.calculate(x)"] -->|Step Into| B["Inside helper.calculate(x)<br/>line-by-line execution"]
    A -->|Step Over| C["Line N+1<br/>(calculate() runs to completion<br/>without stepping through it)"]
    B -->|Step Return| C
```

---

## 6. End-to-End Debug Session Walkthrough

Putting all four sub-sections together — the complete journey from "I want to debug this class" to "I can see live table data and step through logic":

```mermaid
flowchart TD
    Start(["Developer needs to debug<br/>an OpenJVS plug-in"]) --> Precond{"Is Eclipse launched<br/>from the Endur 'Eclipse IDE' task?"}
    Precond -- No --> Launch["Close Eclipse, relaunch via<br/>the Endur 'Eclipse IDE' task"]
    Launch --> Precond
    Precond -- Yes --> Init["11.1: OpenLink → Debug Remote<br/>(or Local) Project → choose session<br/>→ choose JVM (Trading Manager<br/>or Script Engine x)"]
    Init --> JvmCheck{"JVM available?"}
    JvmCheck -- "No JVM Initialized" --> InitJvm["11.2: Run any OpenJVS task<br/>OR use System Monitor →<br/>Debug → Init Java VM,<br/>then retry 11.1"]
    InitJvm --> Init
    JvmCheck -- Yes --> Connected["✅ Debug session connected to JVM"]
    Connected --> SetBP["11.3: Set breakpoints in the<br/>Eclipse source (must match<br/>what's imported into Endur)"]
    SetBP --> RunScript["11.4: Run the script from<br/>Trading Manager (or other means)"]
    RunScript --> Hit["Breakpoint hit → 'Confirm<br/>Perspective Switch' → Yes"]
    Hit --> Inspect["Debug perspective opens:<br/>• Debug View — thread/call stack<br/>• Variable/Breakpoint/Expression views<br/>• Display view — viewTable() + Ctrl+Shift+I"]
    Inspect --> Control["Use toolbar: Continue / Step Into /<br/>Step Over / Step Return / Terminate"]
    Control --> Done(["Bug identified and/or<br/>logic verified"])
```

---

## 7. Practical Checklist for Developers

- [ ] **Launch Eclipse via the Endur `Eclipse IDE` task**, never as a standalone Eclipse instance, before attempting to debug.
- [ ] **Use `Debug Remote Project`**, falling back to `Debug Local Project` if it doesn't work.
- [ ] **Choose your own session** in the Select User Session window unless specifically debugging code on an app server.
- [ ] **Attach to `Trading Manager` for Parameter plug-ins**, and **`Script Engine x` for everything else**.
- [ ] **If no JVM is listed**, either run any OpenJVS task first, or use **System Monitor → Debug → Init Java VM**, then restart the debug session.
- [ ] **Verify the Eclipse source file is identical** to what's been imported into Endur before trusting breakpoint line numbers/variable state.
- [ ] **Set breakpoints by double-clicking the left margin**; use right-click → **Breakpoint Properties** for conditional/hit-count breakpoints when debugging loops over many deals/rows.
- [ ] **Confirm the "Perspective Switch" dialog with `Yes`** to reach the full Debug perspective.
- [ ] **Use the Display view + `viewTable()` + `Ctrl+Shift+I`** to inspect the real contents of any in-scope `Table` object — this is the fastest way to verify data correctness mid-script.
- [ ] **Use Step Into vs. Step Over deliberately** — step into unfamiliar/suspect code, step over well-trusted framework calls (e.g., `Log`, `DestroyableObjectStore`) to avoid noise.

---

## Related Sections (Full Programming Guide)

- Section 5 — OpenJVS Architecture (`IntPtr`/`Table` — explains the `jvsCPtr`/`jvsOwnPtr` fields visible in the Variable View)
- Section 6 — The Common Framework (`BasicScript.execute(IContainerContext)` — visible in the Debug View's call stack)
- Section 10 — Importing Code to Endur (all three import methods must keep Endur and Eclipse source in sync for breakpoints to work correctly)
- Section 12 — Source Control (checking out the correct project/branch in Eclipse before debugging)
