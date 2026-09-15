---
description: "Use when explaining, implementing, or reviewing how OpenJVS source code is organized and checked out from the Subversion repository for Endur Fenix development: repository folder structure, trunk/branches layout, project-to-repository mapping, and checking out supporting projects (e.g. Common) alongside a business project. Trigger keywords: Source Control, Subversion, SVN, SourceCode/Development/OpenJVS, trunk, branches, checkout, Eclipse Java project."
name: "Source Control"
applyTo: "**/*.java"
---

# Source Control — Full Reference Guide

> Source: Section 12 *"Source Control"* of the **Programming Guide for OpenJVS Development — EET Fenix Endur**. This instruction file expands that section into a complete, diagrammed reference explaining how OpenJVS source code is organized in Subversion, and how to correctly check out a project (plus its supporting dependencies) before starting development.

---

## Table of Contents

1. [Overview: Subversion as the System of Record](#1-overview-subversion-as-the-system-of-record)
2. [The Repository Folder Structure](#2-the-repository-folder-structure)
3. [Project-to-Repository Mapping](#3-project-to-repository-mapping)
4. [Trunk vs. Branches](#4-trunk-vs-branches)
5. [Preparing for Development: Checking Out the Right Projects](#5-preparing-for-development-checking-out-the-right-projects)
6. [Worked Example: Developing in the Sims Project](#6-worked-example-developing-in-the-sims-project)
7. [How Source Control Ties Together the Whole Guide](#7-how-source-control-ties-together-the-whole-guide)
8. [Practical Checklist for Developers](#8-practical-checklist-for-developers)

---

## 1. Overview: Subversion as the System of Record

> **Subversion** is the source control software used by the Endur Fenix team. This section **assumes familiarity with Subversion** and how it is used in the Eclipse IDE. See the **separate Eclipse IDE guide** for more details.

Section 12 is deliberately the **final** section of the Programming Guide — it exists to make one point unmistakably clear: **everything described in Sections 4–11 (projects, the Common Framework, plug-in development, importing code, debugging) is built on top of Subversion being the authoritative source of truth.**

```mermaid
graph TD
    SVN[("Subversion Repository<br/>(system of record)")] --> Eclipse["Eclipse IDE<br/>(checked-out Java projects)"]
    Eclipse --> Develop["Develop / edit OpenJVS classes<br/>(Section 8)"]
    Develop --> Import["Import into Endur<br/>(Section 10 — ideally via<br/>Eclipse IDE OpenJVS plugin)"]
    Import --> Debug["Debug in Endur<br/>(Section 11)"]
    Debug --> Commit["Commit verified changes<br/>back to SVN"]
    Commit --> SVN

    style SVN fill:#cdeafe
```

This is why Section 10.1 (Endur Simple Editor) carried such a strong warning: editing directly in Endur **bypasses this entire cycle**, breaking the guarantee that Subversion always reflects what's actually running.

---

## 2. The Repository Folder Structure

> **OpenJVS source code is stored in the Subversion repository as Eclipse projects.** The Eclipse projects **match the OpenJVS projects that can be found in Endur**. The projects are located below the folder **`SourceCode/Development/OpenJVS`** within the **trunk and branches**.

### The Repository Tree (as shown in the guide)

```
SourceCode/
└── Development/
    ├── AVS/
    ├── Configuration/
    ├── Connex/
    ├── Curve_Imports/
    ├── Interfaces/
    ├── Maintenance/
    └── OpenJVS/
        ├── Common/
        ├── DbPurge/
        ├── OneOff/
        ├── Other/
        └── Sims/
```

```mermaid
graph TD
    SourceCode["SourceCode/"] --> Development["Development/"]
    Development --> AVS["AVS/"]
    Development --> Configuration["Configuration/"]
    Development --> Connex["Connex/"]
    Development --> CurveImports["Curve_Imports/"]
    Development --> Interfaces["Interfaces/"]
    Development --> Maintenance["Maintenance/"]
    Development --> OpenJVS["OpenJVS/  ← this guide's focus"]

    OpenJVS --> Common["Common/"]
    OpenJVS --> DbPurge["DbPurge/"]
    OpenJVS --> OneOff["OneOff/"]
    OpenJVS --> Other["Other/"]
    OpenJVS --> Sims["Sims/"]

    style OpenJVS fill:#cdeafe
    style Common fill:#e8f7d4
```

**Key observation:** `Development/` contains **sibling folders for every kind of Endur-related source** (AVS scripts, Configuration, Connex, Curve Imports, Interfaces, Maintenance) — and **`OpenJVS`** is just one of those siblings. Everything this Programming Guide describes lives specifically under the `OpenJVS` subtree.

---

## 3. Project-to-Repository Mapping

The single most important architectural fact in this section:

> **Each of the Endur OpenJVS projects are reflected in the Subversion Source Control repository** (as already noted in Section 4.1). The Eclipse projects **match** the OpenJVS projects found in Endur **exactly**.

This creates a clean **1:1 mapping** between three different "views" of the same project:

```mermaid
graph LR
    subgraph EndurView["Endur (Runtime) View"]
        EndurProj["Project: Sims<br/>Plugin Path:<br/>Plugins/Site/Sims/"]
    end
    subgraph SvnView["Subversion (Repository) View"]
        SvnProj["SourceCode/Development/<br/>OpenJVS/Sims/"]
    end
    subgraph EclipseView["Eclipse (Workspace) View"]
        EclipseProj["Eclipse Java Project: Sims<br/>(checked out from SVN)"]
    end

    EndurProj <-.mirrors.-> SvnProj
    SvnProj <-.checked out as.-> EclipseProj
    EclipseProj -.imported via Section 10.-> EndurProj
```

| View | Where it lives | Purpose |
|---|---|---|
| **Endur (runtime)** | `Plugins/Site/<ProjectName>/` | Where compiled classes actually execute |
| **Subversion (repository)** | `SourceCode/Development/OpenJVS/<ProjectName>/` | The authoritative, versioned source of truth |
| **Eclipse (workspace)** | A checked-out Eclipse Java project | Where a developer actively edits code |

This table directly reinforces Section 4.2's Directory Browser note that the "Plugin Path" and Subversion location are two representations of the **same logical project**.

---

## 4. Trunk vs. Branches

> The projects are located below the folder `SourceCode/Development/OpenJVS` **within the trunk and branches**.

```mermaid
graph TD
    Repo[("Subversion Repository")] --> Trunk["trunk/<br/>SourceCode/Development/OpenJVS/&lt;Project&gt;/<br/>(mainline development)"]
    Repo --> Branches["branches/<br/>e.g. RB_PnL_Project/<br/>SourceCode/Development/OpenJVS/&lt;Project&gt;/<br/>(isolated release/feature work)"]
    Repo --> Tags["tags/<br/>(e.g. Development Tags,<br/>Deployment Docs)"]

    style Trunk fill:#cdeafe
    style Branches fill:#fff2cc
```

This matches what's visible in the Section 10.2 Eclipse Project Explorer screenshots from the guide, where multiple parallel checkouts of the same project exist:

- `Common [not associated] [branches/R8_PnL_Project/SourceCode/Development/...]`
- `EET SVN Development Tags [tags/Dev...]`
- `EET SVN PnL Branch [branches/R8_PnL_Project/...]`
- `EET SVN PnL Branch Deployment Doc`
- `GO`

```mermaid
flowchart TD
    Question{"Which SVN location<br/>should I check out?"}
    Question -- "Standard, current<br/>mainline development" --> UseTrunk["Check out from trunk/<br/>SourceCode/Development/OpenJVS/&lt;Project&gt;"]
    Question -- "Working on an isolated<br/>release/feature branch<br/>(e.g. a specific project rollout)" --> UseBranch["Check out from the relevant<br/>branches/&lt;BranchName&gt;/<br/>SourceCode/Development/OpenJVS/&lt;Project&gt;"]
```

**Practical implication:** always confirm with the team/release plan whether current work should target **trunk** or a specific **branch** before checking anything out — checking out the wrong one leads to changes landing in the wrong release stream.

---

## 5. Preparing for Development: Checking Out the Right Projects

### The Core Rule

> To prepare for development, the **whole OpenJVS project must be checked out from SVN as an Eclipse Java project plus any supporting projects**.

This is the critical, easy-to-miss rule of Section 12: **checking out just the project you intend to edit is not enough** — you must **also** check out every project it depends on (per the Project Plugin Manager's configured references — see Section 4.1), otherwise the Eclipse project won't compile (missing classes from `Common`, `CommonConfig`, etc.).

```mermaid
flowchart TD
    Start(["I need to develop<br/>a class in project X"]) --> CheckX["Check out<br/>SourceCode/Development/OpenJVS/X<br/>as an Eclipse Java project"]
    CheckX --> Refs{"Does project X reference<br/>other projects in the<br/>Project Plugin Manager?<br/>(e.g. Common)"}
    Refs -- Yes --> CheckSupport["ALSO check out each<br/>referenced project<br/>(e.g. SourceCode/Development/<br/>OpenJVS/Common)<br/>as its own Eclipse Java project"]
    Refs -- No --> Ready
    CheckSupport --> Ready(["✅ Eclipse workspace has all<br/>required projects — compiles cleanly"])
```

### Worked Example (from the guide, verbatim logic)

> Therefore if development is required for classes within the **`Sims`** project, then the repository location **`SourceCode/Development/OpenJVS/Sims`** needs to be checked out from SVN as an Eclipse Java project. Since the **`Sims`** project **uses the Common framework**, the repository location **`SourceCode/Development/OpenJVS/Common`** also needs to be checked out from SVN as an Eclipse Java project.

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant SVN as Subversion Repository
    participant Eclipse as Eclipse Workspace

    Dev->>SVN: Check out SourceCode/Development/OpenJVS/Sims
    SVN-->>Eclipse: Sims Eclipse Java project created
    Dev->>Eclipse: Attempt to build Sims project
    Eclipse-->>Dev: ❌ Compile errors — missing BasicScript,\nLog, DestroyableObjectStore, etc.\n(these live in Common)
    Dev->>SVN: Check out SourceCode/Development/OpenJVS/Common
    SVN-->>Eclipse: Common Eclipse Java project created
    Dev->>Eclipse: Rebuild Sims project (now references Common)
    Eclipse-->>Dev: ✅ Builds successfully
```

> The **separate Eclipse IDE guide** provides details how to check out a java project from Subversion.

---

## 6. Worked Example: Developing in the Sims Project

Combining Section 12 with what's already known about the `Sims` project from Section 4.1 (the `Project Plugin Manager` screenshot showing `Sims` referencing `Common`, `APM`, `DbPurge`, `OneOff`, `Other`, `connex`):

```mermaid
graph TD
    subgraph PPM["Project Plugin Manager (Endur) — Sims references"]
        Common["Common ✔ (selected)"]
        APM["APM"]
        DbPurge["DbPurge"]
        OneOff["OneOff"]
        Other["Other"]
        Connex["connex"]
    end

    subgraph Checkout["What must actually be checked out from SVN?"]
        SimsProj["SourceCode/Development/OpenJVS/Sims<br/>(the project being developed)"]
        CommonProj["SourceCode/Development/OpenJVS/Common<br/>(REQUIRED — Sims references it)"]
    end

    Common -.corresponds to.-> CommonProj
    SimsProj --> Checkout
    CommonProj --> Checkout

    style CommonProj fill:#c9f7c5
```

**Important nuance:** the Project Plugin Manager screenshot shows **only `Common` ticked** as an active reference for `Sims` (the others — `APM`, `DbPurge`, `OneOff`, `Other`, `connex` — are listed as *available* references but not currently selected). This means, in this specific example, **only `Sims` and `Common` strictly need to be checked out** — not every project listed in the Plugin Manager's reference list, only the ones actually **ticked/selected**.

```mermaid
flowchart TD
    Q{"Is the candidate project<br/>TICKED/selected as an<br/>active reference in the<br/>Project Plugin Manager?"}
    Q -- Yes --> MustCheckout["Must also check this project<br/>out from SVN"]
    Q -- No --> SkipCheckout["Not required for this<br/>development task<br/>(even if listed as available)"]
```

---

## 7. How Source Control Ties Together the Whole Guide

Section 12 is the natural capstone to the entire Programming Guide — every prior section assumed this SVN structure existed:

| Earlier Section | How It Depends on Source Control |
|---|---|
| **4.1 OpenJVS Projects** | Stated project references are configured in the Plugin Manager — Section 12 explains *where* the actual source for each referenced project lives |
| **4.2 Directory Browser** | Warned that Simple Editor changes are "not reflected in Subversion source control" — Section 12 explains exactly what that repository looks like |
| **6.1 Projects of the Common Framework** | `CommonConfig`, `Common`, `CommonExtension` are each their own SVN-mirrored project |
| **7. CommonExtension Shared Package** | The governance rule (no cross-referencing business projects) is enforced partly by projects living in **separate, independent SVN locations** — makes it structurally awkward to "reach into" another project's source |
| **10.2/10.3 Importing Code** | Both import paths assume the developer's Eclipse workspace already has the correct SVN-checked-out projects open |
| **11.3 Setting Breakpoints** | Requires the Eclipse source (from SVN) to be **identical** to what's imported into Endur — only possible if the correct SVN checkout was used in the first place |

```mermaid
graph TD
    Sec12["Section 12: Source Control<br/>(SVN structure + checkout rules)"] --> Sec4["Section 4: Project references<br/>rely on knowing where each<br/>project's SVN source lives"]
    Sec12 --> Sec6["Section 6: Common Framework<br/>projects are each independently<br/>versioned in SVN"]
    Sec12 --> Sec7["Section 7: CommonExtension<br/>governance is reinforced by<br/>SVN's project-per-folder isolation"]
    Sec12 --> Sec10["Section 10: Importing Code<br/>assumes correct SVN checkout<br/>already exists in Eclipse"]
    Sec12 --> Sec11["Section 11: Debugging<br/>requires Eclipse source (SVN)<br/>to match Endur exactly"]

    style Sec12 fill:#cdeafe
```

---

## 8. Practical Checklist for Developers

- [ ] **Confirm you're working in the correct SVN location** — `trunk` for mainline work, or the specific `branches/<BranchName>` for isolated release/feature work.
- [ ] **Always check out from `SourceCode/Development/OpenJVS/<ProjectName>`** as an Eclipse Java project — never assume a project already exists correctly in your workspace.
- [ ] **Before starting development, check the Project Plugin Manager** (Section 4.1) to see which other projects are **ticked/selected** as active references for your target project.
- [ ] **Check out every actively-referenced project** (most commonly `Common`) as its own separate Eclipse Java project — otherwise the build will fail with missing-class compile errors.
- [ ] **Never rely on the Endur Simple Editor** (Section 10.1) as a substitute for this SVN checkout workflow — it explicitly does not sync with source control.
- [ ] **Keep Eclipse source and Endur-imported classes in sync** — this SVN-based checkout is the foundation that makes debugging (Section 11.3) reliable.
- [ ] **Consult the separate Eclipse IDE guide** for the exact mechanical steps of performing an SVN checkout within Eclipse (right-click → Team / SVN checkout wizard), since this Programming Guide intentionally defers those details to that guide.

---

## Related Sections (Full Programming Guide)

- Section 4.1 — OpenJVS Projects (Project Plugin Manager references — determines which other projects must also be checked out)
- Section 4.2 — Directory Browser (why Simple Editor changes are "not reflected in Subversion source control")
- Section 6.1 — Projects of the Common Framework (`CommonConfig`, `Common`, `CommonExtension` as independently SVN-mirrored projects)
- Section 7 — The CommonExtension Project Shared Package (governance rule reinforced by SVN's per-project isolation)
- Section 10 — Importing Code to Endur (all import paths assume a correct SVN checkout already exists in Eclipse)
- Section 11.3 — Setting Breakpoints (requires Eclipse source to be identical to what was imported into Endur)
