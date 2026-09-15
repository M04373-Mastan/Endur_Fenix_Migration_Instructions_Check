---
description: "Use when explaining, implementing, or reviewing the OpenJVS/Endur Data Purging Framework: AbstractDbPurgeSet, DbPurgeDailyMain/WeeklyMain/MonthlyMain/AnnuallyMain, the IPurgeTable interface, user table purging, core table purging, and the DbPurge Constants Repository entries. Trigger keywords: Data Purging, Purging Framework, AbstractDbPurgeSet, DbPurgeDailyMain, DbPurgeWeeklyMain, DbPurgeMonthlyMain, DbPurgeAnnuallyMain, IPurgeTable, purgeTable, user_eon_configuration, Purge Batch Size, Max Purge Rows Per Table."
name: "Data Purging"
applyTo: "**/*.java"
---

# Data Purging — Full Reference Guide

> Source: Section 9 *"Data Purging"* of the **Programming Guide for OpenJVS Development — EET Fenix Endur**, covering 9.1 The Purging Framework (9.1.1 AbstractDbPurgeSet Class, 9.1.2 DbPurge[Daily/Weekly/Monthly/Annually]Main Classes, 9.1.3 IPurgeTable Interface), 9.2 User Table Purging, 9.3 Core Table Purging, and 9.3.1 Purging Framework Constants Repository Entries. This instruction file expands the section into a complete, diagrammed reference.

---

## Table of Contents

1. [Overview: What the Purging Framework Solves](#1-overview-what-the-purging-framework-solves)
2. [9.1 The Purging Framework](#2-91-the-purging-framework)
   - 9.1.1 [AbstractDbPurgeSet Class](#211-abstractdbpurgeset-class)
   - 9.1.2 [DbPurge\[Daily/Weekly/Monthly/Annually\]Main Classes](#212-dbpurgedailyweeklymonthlyannuallymain-classes)
   - 9.1.3 [IPurgeTable Interface](#213-ipurgetable-interface)
3. [9.2 User Table Purging](#3-92-user-table-purging)
4. [9.3 Core Table Purging](#4-93-core-table-purging)
   - 9.3.1 [Purging Framework Constants Repository Entries](#41-purging-framework-constants-repository-entries)
5. [End-to-End Purge Run Walkthrough](#5-end-to-end-purge-run-walkthrough)
6. [Practical Checklist for Developers](#6-practical-checklist-for-developers)

---

## 1. Overview: What the Purging Framework Solves

Endur databases accumulate data continuously — deals, transactions, logs, index prices, import audit records, and custom EET user tables all grow without bound unless something proactively trims them. The **Purging Framework** is the Common Framework's answer to this problem:

> The purging framework has been designed as a **one stop place** where all database table purging is carried out. The framework is able to purge **both Endur core tables and custom user tables** at a **daily, weekly, monthly and annually** frequency. The framework consists of **control classes** that initiate and execute **worker classes** that carry out the actual table purging.

```mermaid
graph TD
    subgraph Control["Control Classes (schedule + orchestrate)"]
        AbstractDbPurgeSet["AbstractDbPurgeSet"]
        Daily["DbPurgeDailyMain"]
        Weekly["DbPurgeWeeklyMain"]
        Monthly["DbPurgeMonthlyMain"]
        Annually["DbPurgeAnnuallyMain"]
    end
    subgraph Worker["Worker Classes (do the actual purging)"]
        W1["IPurgeTable implementations<br/>(one per user table)"]
        W2["Core table purge logic<br/>(driven by Endur purge codes)"]
    end
    subgraph Data["Data"]
        Core[("Endur Core Tables")]
        User[("EET Custom User Tables")]
    end

    AbstractDbPurgeSet --> Daily
    AbstractDbPurgeSet --> Weekly
    AbstractDbPurgeSet --> Monthly
    AbstractDbPurgeSet --> Annually
    Daily --> W1
    Weekly --> W1
    Monthly --> W1
    Annually --> W1
    Daily --> W2
    Weekly --> W2
    Monthly --> W2
    Annually --> W2
    W1 --> User
    W2 --> Core

    style Control fill:#cdeafe
    style Worker fill:#e8f7d4
```

**Two distinct purge targets, one unified framework:**

| Purge Target | Configured Via | Worker Mechanism |
|---|---|---|
| **Core Endur tables** | `user_eon_configuration` parameters + **Endur purging configuration** | Endur purge codes (e.g. `ADT_PURGE_CANCELLED_DEAL`) |
| **Custom EET user tables** | Developer-written classes | `IPurgeTable` interface implementations |

---

## 2. 9.1 The Purging Framework

### 2.1 AbstractDbPurgeSet Class

> **Abstract class** from which the purging **set classes** extend. The current purging sets include **`DbPurgeDailyMain`, `DbPurgeWeeklyMain`, `DbPurgeMonthlyMain`** and **`DbPurgeAnnuallyMain`**. This abstract class **extends `BasicScript`** and provides the implementation of the **`execute`** method. The execute method **iterates through each registered worker class** and calls the worker's **`purgeTable`** method. Each worker class needs to be registered by the purging set class using the **`registerNewClass`** method of this abstract class.

```mermaid
classDiagram
    class BasicScript {
        <<abstract>>
        +execute(Table argt, Table returnt) void*
    }
    class AbstractDbPurgeSet {
        <<abstract>>
        +execute(Table argt, Table returnt) void
        +registerNewClass(IPurgeTable worker) void
        #initTablePurge() void*
    }
    class IPurgeTable {
        <<interface>>
        +purgeTable(List tables) void
    }
    BasicScript <|-- AbstractDbPurgeSet
    AbstractDbPurgeSet --> IPurgeTable : iterates & calls purgeTable() on
```

```mermaid
sequenceDiagram
    participant Endur
    participant PurgeSet as AbstractDbPurgeSet subclass<br/>(e.g. DbPurgeDailyMain)
    participant Worker1 as IPurgeTable worker #1
    participant Worker2 as IPurgeTable worker #2
    participant DB as Endur Database

    Endur->>PurgeSet: execute(argt, returnt)  [via BasicScript]
    activate PurgeSet
    PurgeSet->>PurgeSet: initTablePurge()<br/>registerNewClass(worker1)<br/>registerNewClass(worker2)
    loop for each registered worker class
        PurgeSet->>Worker1: purgeTable(tableList)
        Worker1-->>PurgeSet: memory table(s) with rows to purge
        PurgeSet->>Worker2: purgeTable(tableList)
        Worker2-->>PurgeSet: memory table(s) with rows to purge
    end
    PurgeSet->>DB: purge rows using collected memory tables
    deactivate PurgeSet
```

**Key architectural point:** `AbstractDbPurgeSet` is the **orchestrator** — it knows *how* to run a purge cycle (iterate workers, call `purgeTable`, apply the results to the database) but knows **nothing** about *which* tables to purge. That knowledge lives entirely in the registered `IPurgeTable` worker classes.

### 2.2 DbPurge[Daily/Weekly/Monthly/Annually]Main Classes

> These classes define purging sets that run either **daily, weekly, monthly or annually**. They extend the **`AbstractDbPurgeSet`** class. The classes are referred to in the **Endur workflow** with appropriate run schedules.

**⚠️ Special case — the Annual purge:**

> Note that **Endur does not provide an annual schedule**, therefore the annual purging set is **scheduled to run every day** and checks the parameter **`'Annual DB Purge Date'`** (found in the **`user_eon_configuration`** table) to determine the **day of the year** the purge should actually run.

```mermaid
flowchart TD
    Daily["DbPurgeDailyMain<br/>(Endur schedule: daily)"] --> RunDaily["Runs every day"]
    Weekly["DbPurgeWeeklyMain<br/>(Endur schedule: weekly)"] --> RunWeekly["Runs once per week"]
    Monthly["DbPurgeMonthlyMain<br/>(Endur schedule: monthly)"] --> RunMonthly["Runs once per month"]
    Annually["DbPurgeAnnuallyMain<br/>(Endur schedule: DAILY —<br/>no native annual schedule exists!)"] --> CheckDate{"Today == 'Annual DB<br/>Purge Date' parameter<br/>in user_eon_configuration?"}
    CheckDate -- Yes --> RunAnnual["Actually perform<br/>the annual purge"]
    CheckDate -- No --> SkipAnnual["Do nothing, exit"]

    style Annually fill:#fff2cc
    style CheckDate fill:#fff2cc
```

### Registering Worker Classes

> Worker classes that are to be included in these purging sets need to be **instantiated and registered** with the set by including the following code in the purging set's **`initTablePurge`** method:

```java
registerNewClass(new DbPurgeLogDetail());
```

...where `DbPurgeLogDetail` is the worker class.

```mermaid
classDiagram
    class AbstractDbPurgeSet {
        <<abstract>>
        #initTablePurge() void*
        +registerNewClass(IPurgeTable) void
    }
    class DbPurgeDailyMain {
        #initTablePurge() void
        "registerNewClass(new DbPurgeLogDetail())"
    }
    class DbPurgeLogDetail {
        <<IPurgeTable worker>>
        +purgeTable(List tables) void
    }
    AbstractDbPurgeSet <|-- DbPurgeDailyMain
    DbPurgeDailyMain ..> DbPurgeLogDetail : registers instance of
```

### 2.3 IPurgeTable Interface

> The interface that **must be implemented by all purging worker classes**. The interface defines the method **`purgeTable`** that must be implemented by the worker class. This method should **retrieve data to be purged into one or more memory tables**, which must be **added to the list object** provided to the method as a parameter. The framework then **uses the tables in the list object to purge the database tables**.

```mermaid
classDiagram
    class IPurgeTable {
        <<interface>>
        +purgeTable(List~Table~ tables) void
    }
    class DbPurgeLogDetail {
        +purgeTable(List~Table~ tables) void
    }
    class DbPurgeUserIndexPriceOutputValues {
        +purgeTable(List~Table~ tables) void
    }
    class DbPurgeIdxImportAudit {
        +purgeTable(List~Table~ tables) void
    }

    IPurgeTable <|.. DbPurgeLogDetail
    IPurgeTable <|.. DbPurgeUserIndexPriceOutputValues
    IPurgeTable <|.. DbPurgeIdxImportAudit
```

```mermaid
sequenceDiagram
    participant PurgeSet as AbstractDbPurgeSet
    participant Worker as IPurgeTable worker
    participant Query as Retention Query Logic
    participant DB as Database

    PurgeSet->>Worker: purgeTable(tableList)
    activate Worker
    Worker->>Query: determine retention date / cut-off
    Worker->>DB: SELECT rows older than retention date
    DB-->>Worker: candidate rows
    Worker->>Worker: build memory table(s) of rows to purge
    Worker->>PurgeSet: tableList.add(memoryTable)
    deactivate Worker
    PurgeSet->>DB: DELETE using the memory table(s)
```

---

## 3. 9.2 User Table Purging

> **User tables that increase in size over a period of time should be purged on a regular basis.** Depending on the contents of the user table, the purge should be **daily, weekly, monthly or annually**. To define a user table purge you must:

| Step | Action |
|---|---|
| **1** | Create a class that implements **`IPurgeTable`**. The class name must **represent the user table being purged** and must be contained within the package **`com.eon.eet.fenix.purging`**. **Only one user table should be purged per purging class.** |
| **2** | Implement the **`purgeTable`** method to provide memory tables that contain data to purge. |
| **3** | Modify the **`initTablePurge`** method of the purging set class associated with the frequency the user table is to be purged (`DbPurgeDailyMain`, `DbPurgeWeeklyMain`, `DbPurgeMonthlyMain`, or `DbPurgeAnnuallyMain`). The method needs to **register an instance** of the new purging class. |

```mermaid
flowchart TD
    Start(["New EET user table needs periodic purging"]) --> Step1["1. Create class implementing IPurgeTable\nPackage: com.eon.eet.fenix.purging\nName represents the table\n(ONE table per class)"]
    Step1 --> Step2["2. Implement purgeTable(List tables)\nto select rows to purge into memory table(s)"]
    Step2 --> Step3{"What frequency does\nthis table need?"}
    Step3 -- Daily --> RegD["Add registerNewClass(new YourWorker())\nto DbPurgeDailyMain.initTablePurge()"]
    Step3 -- Weekly --> RegW["Add to DbPurgeWeeklyMain.initTablePurge()"]
    Step3 -- Monthly --> RegM["Add to DbPurgeMonthlyMain.initTablePurge()"]
    Step3 -- Annually --> RegA["Add to DbPurgeAnnuallyMain.initTablePurge()"]
    RegD --> Done(["Table now purged automatically\non the chosen schedule"])
    RegW --> Done
    RegM --> Done
    RegA --> Done
```

### Worked Example

```mermaid
classDiagram
    class IPurgeTable {
        <<interface>>
        +purgeTable(List tables) void
    }
    class DbPurgeUserIndexPriceOutputValues {
        "Package: com.eon.eet.fenix.purging\nPurges ONE table: user_index_price_output_values"
        +purgeTable(List tables) void
    }
    class DbPurgeMonthlyMain {
        #initTablePurge() void
        "registerNewClass(new DbPurgeUserIndexPriceOutputValues())"
    }
    IPurgeTable <|.. DbPurgeUserIndexPriceOutputValues
    DbPurgeMonthlyMain ..> DbPurgeUserIndexPriceOutputValues : registers
```

---

## 4. 9.3 Core Table Purging

> The purging framework uses the **Endur purging configuration** to determine **datasets and retaining dates** for purging the core tables. The purging framework uses parameters in the **`user_eon_configuration`** table to determine **which core tables should be purged and when**.

### The Four Parameter Names

> The parameter names are as follows and reflect the purge frequency:

- **Purge Data Types Daily**
- **Purge Data Types Monthly**
- **Purge Data Types Weekly**
- **Purge Data Types Annually**

> The value of each parameter is a **comma separated list of Endur purge codes** as found in the Endur purging configuration, e.g.

```
ADT_PURGE_CANCELLED_DEAL,ADT_PURGE_DELETED_DEAL
```

> To include a core table purge, simply **add the Endur purge code** to the relevant parameter and ensure it is **correctly configured in the Endur purging configuration**.

```mermaid
flowchart TD
    Config["user_eon_configuration table"] --> P1["Purge Data Types Daily =<br/>ADT_PURGE_CANCELLED_DEAL,<br/>ADT_PURGE_DELETED_DEAL"]
    Config --> P2["Purge Data Types Weekly = ..."]
    Config --> P3["Purge Data Types Monthly = ..."]
    Config --> P4["Purge Data Types Annually = ..."]

    P1 --> DailyRun["DbPurgeDailyMain reads this list<br/>and purges matching core tables<br/>via Endur purging configuration"]

    subgraph EndurPurgeConfig["Endur Purging Configuration (native)"]
        Codes["Purge codes define which<br/>tables/datasets + retention rules<br/>each code represents"]
    end
    P1 -.uses codes defined in.-> EndurPurgeConfig
```

### Failure/Warning Behavior

> If any of the parameters **does not exist** in the `user_eon_configuration` table, or the parameter value is **empty**, then **no purging is carried out** and a **warning message is output** to the appropriate purging script's log file.

```mermaid
flowchart TD
    Check{"Parameter exists in<br/>user_eon_configuration<br/>AND has a non-empty value?"}
    Check -- Yes --> Purge["Purge the listed core<br/>tables/datasets per<br/>Endur purging configuration"]
    Check -- No --> Warn["⚠️ No purging carried out.<br/>Warning message logged to<br/>the purging script's log file"]
```

### 4.1 Purging Framework Constants Repository Entries

The purging framework's own *operational* controls (as opposed to *what* to purge, which is `user_eon_configuration`'s job) live in the **Constants Repository**, under context **`DbPurge`**:

| Context | Sub Context | Variable Name | Type | Description |
|---|---|---|---|---|
| `DbPurge` | — | **Disabled Purge Classes** | String | A **comma separated list** of `IPurgeTable` classes to be **disabled** from running during a purge run. |
| `DbPurge` | — | **Max Purge Rows Per Table** | Integer | Maximum number of rows allowed to be purged **per table in a single purge run**. |
| `DbPurge` | — | **Purge Batch Size** | (unspecified/numeric) | Number of rows to purge in a **single batch**. E.g. if set to `10` and 100 rows need purging, the purge runs in **10 batches of 10 rows** until all 100 are purged. |

```mermaid
classDiagram
    class DbPurgeConstantsRepository {
        <<Constants Repository, context=DbPurge>>
        Disabled Purge Classes : String (comma-separated)
        Max Purge Rows Per Table : Integer
        Purge Batch Size : Integer
    }
```

```mermaid
flowchart TD
    Run(["Purge run starts for a table"]) --> Q1{"Is this IPurgeTable class<br/>listed in 'Disabled Purge Classes'?"}
    Q1 -- Yes --> Skip["Skip this worker entirely"]
    Q1 -- No --> Q2{"Total rows to purge ><br/>'Max Purge Rows Per Table'?"}
    Q2 -- Yes --> Cap["Cap purge at the configured maximum"]
    Q2 -- No --> Full["Purge all eligible rows"]
    Cap --> Batch["Purge in batches of<br/>'Purge Batch Size' rows at a time"]
    Full --> Batch
    Batch --> Repeat{"More rows remaining<br/>(up to the cap)?"}
    Repeat -- Yes --> Batch
    Repeat -- No --> Done(["Purge run complete for this table"])
```

**Why batching matters:** purging 100,000 rows in one giant `DELETE` can lock tables for a long time and strain the database. `Purge Batch Size` breaks the work into small, manageable chunks (e.g., 10 rows at a time) so the purge is gentler on the live database.

**Why "Max Purge Rows Per Table" matters:** protects against runaway purges — e.g., if a retention-date bug accidentally flags millions of rows, the cap prevents a single purge run from doing catastrophic, unbounded deletion.

**Why "Disabled Purge Classes" matters:** gives operations/support staff a way to **temporarily turn off** a misbehaving purge worker (e.g., during an investigation) **without redeploying code** — just update the Constants Repository entry.

---

## 5. End-to-End Purge Run Walkthrough

Putting Sections 9.1–9.3 together — a full daily purge cycle from Endur's scheduler through to the database:

```mermaid
sequenceDiagram
    participant Sched as Endur Workflow Scheduler
    participant Daily as DbPurgeDailyMain<br/>(extends AbstractDbPurgeSet)
    participant CR as Constants Repository<br/>(context=DbPurge)
    participant Config as user_eon_configuration
    participant CoreWorker as Core Table Purge Logic
    participant UserWorker as IPurgeTable workers<br/>(e.g. DbPurgeLogDetail)
    participant DB as Endur Database
    participant Log

    Sched->>Daily: Run scheduled daily task
    Daily->>Daily: initTablePurge()<br/>registerNewClass(...) for each user-table worker
    Daily->>CR: read Disabled Purge Classes,<br/>Max Purge Rows Per Table, Purge Batch Size

    Daily->>Config: read "Purge Data Types Daily" parameter
    alt Parameter missing or empty
        Daily->>Log: log warning — no core purging done
    else Parameter has purge codes
        Daily->>CoreWorker: purge listed Endur purge codes
        CoreWorker->>DB: delete matching core rows<br/>(respecting retention config)
    end

    loop for each registered IPurgeTable worker (not disabled)
        Daily->>UserWorker: purgeTable(tableList)
        UserWorker-->>Daily: memory table(s) of rows to purge
        Daily->>DB: delete rows in batches of<br/>Purge Batch Size, up to<br/>Max Purge Rows Per Table
    end

    Daily->>Log: log purge run summary
```

---

## 6. Practical Checklist for Developers

- [ ] **New EET user table growing unbounded?** Create an `IPurgeTable` implementation in `com.eon.eet.fenix.purging`, named after the table, purging exactly **one** table per class.
- [ ] **Register the worker** in the correct `DbPurge[Daily/Weekly/Monthly/Annually]Main.initTablePurge()` method via `registerNewClass(...)`.
- [ ] **Remember the Annual purge quirk** — `DbPurgeAnnuallyMain` runs *every day* but only actually purges on the date configured in `user_eon_configuration`'s `'Annual DB Purge Date'` parameter.
- [ ] **For core table purging**, add the correct Endur purge code(s) to the relevant `Purge Data Types [Daily/Weekly/Monthly/Annually]` parameter in `user_eon_configuration`, and confirm the code is properly set up in Endur's own purging configuration.
- [ ] **Never assume a missing/empty core-purge parameter fails loudly** — it silently skips purging and only logs a warning; check log files if core purging doesn't seem to be running.
- [ ] **Use the `DbPurge` Constants Repository entries** (`Disabled Purge Classes`, `Max Purge Rows Per Table`, `Purge Batch Size`) to operationally control purge behavior without code changes.
- [ ] **Don't purge in one giant batch** — respect `Purge Batch Size` to avoid long-running locks on production tables.
- [ ] **`purgeTable(List tables)` must add memory tables to the provided list**, not perform deletes itself — the framework (`AbstractDbPurgeSet`) owns the actual database delete step.

---

## Related Sections (Full Programming Guide)

- Section 6 — The Common Framework (`BasicScript`, `DestroyableObjectStore` — `AbstractDbPurgeSet` extends `BasicScript` and purge workers should use `DestroyableObjectStore` for any tables they create)
- Section 8 — Developing OpenJVS Plugins (general naming/package rules that also apply to purge worker classes)
- Section 4.3 — Current EET OpenJVS Projects (the `DbPurge` project: `com.eon.eet.fenix.purging`, "Code related to database purging")
