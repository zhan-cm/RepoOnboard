# RepoOnboard

> Turn unfamiliar repositories into interactive codebase maps.

RepoOnboard is an open-source **codebase comprehension and developer onboarding tool** designed to help developers understand unfamiliar repositories faster.

Instead of manually browsing hundreds of files, tracing dependencies, searching for entry points, and guessing where to start, RepoOnboard analyzes a repository and transforms it into an interactive map of:

- Project structure
- Modules
- Components
- Entry points
- HTTP APIs
- Dependencies
- Architecture
- Recommended reading paths

The long-term goal is simple:

> **Help developers understand a codebase before they modify it.**

---

## Status

> **Early Development / Project Foundation**

RepoOnboard has completed its initial architecture decisions and is entering the project foundation phase.

The first implementation has not been released yet.

The initial version will focus on:

```text
Java
+
Maven
+
Spring Boot
````

Multi-language support is a long-term goal, not a V0.1 requirement.

---

# Why RepoOnboard?

Understanding an unfamiliar repository is often one of the slowest parts of software development.

When joining a new project, reading an open-source codebase, or taking over a legacy system, developers usually need to manually answer questions such as:

* What does this project do?
* Where does the application start?
* Which modules are important?
* Where are the HTTP APIs?
* Which Controllers call which Services?
* Which Services depend on which Repositories?
* Which files should I read first?
* How is the project actually organized?

This often means manually navigating:

```text
README
   ↓
Build configuration
   ↓
Directory structure
   ↓
Application entry point
   ↓
Controllers
   ↓
Services
   ↓
Repositories
   ↓
Configuration
   ↓
More source files...
```

RepoOnboard aims to automate much of this initial exploration.

---

# The Idea

The intended experience is:

```bash
cd unfamiliar-project

repoonboard .
```

RepoOnboard analyzes the repository:

```text
Detecting project...

✓ Maven project detected
✓ Spring Boot detected
✓ Java version detected

Analyzing source code...

✓ Modules discovered
✓ Components discovered
✓ API endpoints discovered
✓ Dependencies extracted
✓ Entry points detected

Building project map...
```

Then it opens a local interactive interface.

```text
                 Project

                    │
          ┌─────────┴─────────┐
          │                   │
       Modules             Entry Points
          │
          ▼
      Controllers
          │
          ▼
       Services
          │
          ▼
   Repositories / Mappers
```

The goal is to help developers build a mental model of the project before diving into implementation details.

---

# Planned V0.1 Features

RepoOnboard V0.1 will initially target:

> **Java + Maven + Spring Boot repositories**

The first version is intentionally focused.

---

## Project Overview

Automatically detect information such as:

```text
Project
demo-shop

Java
21

Spring Boot
3.x

Build System
Maven

Modules
4

Controllers
18

Services
27

Repositories
12

HTTP Endpoints
83
```

---

## Maven Module Map

Understand single-module and multi-module Maven repositories.

Example:

```text
demo-shop

├── shop-common
├── shop-user
├── shop-order
└── shop-payment
```

RepoOnboard will extract information such as:

* Parent modules
* Child modules
* Maven coordinates
* Java version
* Spring Boot version
* Dependencies

---

## Spring Component Discovery

Detect major Spring components such as:

```text
@Controller
@RestController
@Service
@Repository
@Component
```

Planned support also includes common Mapper patterns where practical.

---

## API Map

Automatically extract Spring MVC endpoints.

Example:

```text
GET      /users
POST     /users
GET      /users/{id}
PUT      /users/{id}
DELETE   /users/{id}
```

Each endpoint should be traceable back to its implementation:

```text
POST /users
      │
      ▼
UserController.createUser()
      │
      ▼
src/main/java/.../UserController.java:73
```

---

## Architecture Map

Visualize major component dependencies.

Example:

```text
                UserController
                      │
                      ▼
                  UserService
                  /         \
                 ▼           ▼
        UserRepository    RoleService
                               │
                               ▼
                         RoleRepository
```

The purpose is not to display every class in the repository.

The purpose is to reveal the structure that matters.

---

## Entry Point Detection

Identify important starting points such as:

* Spring Boot application class
* HTTP controllers
* Important configuration classes

Example:

```text
DemoApplication.java

@SpringBootApplication
```

↓

```text
APPLICATION ENTRY POINT
```

---

## Start Here

One of RepoOnboard's core goals is to answer:

> **Where should I start reading this repository?**

Example:

```text
Recommended Reading Path

1. pom.xml

2. DemoApplication.java
   Main application entry point

3. SecurityConfig.java
   Defines authentication architecture

4. LoginController.java
   Main authentication HTTP entry point

5. LoginService.java
   Authentication business logic

6. UserService.java
   Core user domain service
```

Recommendations should be explainable.

RepoOnboard should not simply produce an unexplained ranking.

---

# Planned Interface

The V0.1 interface is expected to contain four primary views.

---

## Overview

Answers:

> What is this project?

Shows:

* Technology stack
* Project metadata
* Maven modules
* Component statistics
* API statistics
* Application entry points

---

## Architecture

Answers:

> How is this project organized?

Shows:

* Modules
* Controllers
* Services
* Repositories
* Mappers
* Important dependencies

---

## API Map

Answers:

> What HTTP APIs does this project expose?

Shows:

* HTTP method
* Endpoint path
* Controller
* Handler method
* Source location

---

## Start Here

Answers:

> What should I read first?

Shows:

* Recommended files
* Reading order
* Reason for each recommendation

---

# Long-Term Vision

RepoOnboard is not intended to remain Spring Boot-only.

The long-term vision is a language-extensible codebase understanding platform.

Potential future ecosystems include:

```text
Java
├── Spring Boot
├── Quarkus
└── Micronaut

TypeScript / JavaScript
├── Node.js
├── Express
├── NestJS
└── Next.js

Python
├── FastAPI
├── Django
└── Flask

Go
├── net/http
├── Gin
└── Fiber

Rust
├── Axum
└── Actix Web
```

The core idea is:

```text
Repository
     ↓
Language / Framework Analyzer
     ↓
Unified Project Model
     ↓
Architecture & Dependency Analysis
     ↓
Interactive Codebase Map
```

---

# Design Principles

RepoOnboard is being designed around several core principles.

---

## Static Analysis First

Information that can be derived reliably from source code should be extracted deterministically.

For example:

```java
@RestController
public class UserController {
}
```

should be detected through source analysis.

It should not require an LLM to guess that the class is a controller.

---

## Accurate Before Impressive

RepoOnboard should prefer:

```text
Correct
Reliable
Traceable
Explainable
```

over:

```text
Looks intelligent
```

A smaller amount of trustworthy information is more valuable than a large amount of speculative analysis.

---

## Local First

Core repository analysis should run locally.

Source code should not need to be uploaded to an external service for basic analysis.

---

## Explainable Results

Important conclusions should be traceable to source code.

For example:

```text
POST /login

LoginController.login()

src/main/java/.../LoginController.java:42
```

Users should be able to understand why RepoOnboard produced a result.

---

## Progressive Understanding

RepoOnboard should present information from high level to low level.

```text
Project
   ↓
Architecture
   ↓
Module
   ↓
Component
   ↓
Source
```

The goal is to reduce information overload.

---

## One Ecosystem Done Well

RepoOnboard will not attempt to support many languages badly in its first release.

V0.1 focuses on:

```text
Java + Maven + Spring Boot
```

Only after the core product value is validated will additional ecosystems be considered.

---

# What RepoOnboard Is Not

RepoOnboard V0.1 is not intended to be:

* An AI coding agent
* An AI code generator
* A code review bot
* A refactoring engine
* A bug fixing system
* A full IDE
* A generic RAG application
* A codebase chatbot
* A cloud SaaS platform

The initial mission is narrower:

> **Understand the repository first.**

---

# AI

RepoOnboard V0.1 will not depend on a large language model.

Future AI features may include:

* Architecture explanations
* Module explanations
* Codebase questions
* Business-flow explanations
* Generated onboarding guides

However, AI should operate on top of RepoOnboard's structured analysis results.

The intended architecture is:

```text
Source Code
    ↓
Static Analysis
    ↓
Project Model
    ↓
Dependency Graph
    ↓
Source Evidence
    ↓
Optional AI Explanation
```

not:

```text
Entire Repository
    ↓
LLM
    ↓
Guess
```

---

# Target Users

RepoOnboard is intended for:

### Developers joining an existing project

Understand a new codebase faster.

### Interns and junior developers

Build a mental model of a real project without blindly browsing directories.

### Open-source contributors

Understand unfamiliar GitHub repositories before submitting the first contribution.

### Students

Study how real-world projects are structured.

### Developers maintaining legacy systems

Quickly discover important modules, entry points, APIs, and dependencies.

---

# Development Roadmap

Current roadmap:

```text
M0
Technical Architecture

↓

M1
Project Foundation

↓

M2
Maven Analysis

↓

M3
Java Source Analysis

↓

M4
Spring Boot Analysis

↓

M5
API & Dependency Analysis

↓

M6
Unified Project Model

↓

M7
Local Web UI

↓

M8
Start Here

↓

M9
Real Repository Validation

↓

M10
V0.1 Release
```

Detailed development tasks are maintained in:

```text
TODO.md
```

Technical decisions are maintained in:

```text
DECISIONS.md
```

Product scope and principles are maintained in:

```text
PROJECT.md
```

---

# V0.1 Definition of Success

RepoOnboard V0.1 will be considered successful if a developer can run:

```bash
repoonboard .
```

against a real Spring Boot Maven repository and obtain:

* Project overview
* Maven module map
* Spring component map
* HTTP API map
* Major dependency relationships
* Application entry points
* Recommended reading path
* Local interactive interface
* Source traceability

But the real success criterion is simpler:

> **Does RepoOnboard help someone understand an unfamiliar repository faster than manually browsing the source code?**

If the answer is yes, the core product works.

---

# Installation

RepoOnboard is not yet available for installation.

Installation instructions will be added once the first usable development build is released.

---

# Usage

RepoOnboard is currently under development.

The intended future usage is:

```bash
cd your-project

repoonboard .
```

---

# Documentation

Project documentation:

* [`PROJECT.md`](./PROJECT.md) — Product vision, scope, and principles
* [`DECISIONS.md`](./DECISIONS.md) — Architecture and technical decisions
* [`TODO.md`](./TODO.md) — Development roadmap and execution status

---

# Contributing

RepoOnboard is currently in its early architecture and implementation stage.

Contribution guidelines will be added once the core architecture is stable enough for external contributions.

Until then, architecture decisions should remain consistent with:

```text
PROJECT.md
DECISIONS.md
```

---

# License

License has not been finalized yet.

A license will be selected before the first public release.

---

# Project Philosophy

RepoOnboard follows four simple rules:

> **Understand first. Modify later.**

> **Static analysis before generative AI.**

> **Accurate before impressive.**

> **One ecosystem done well before many ecosystems done poorly.**

````

这版 README 现在有几个好处。

第一，它**不会欺骗 GitHub 用户**。现在还没代码，所以明确写：

```text
Early Development / Technical Architecture Design
````

而不是搞：

```text
Installation
npm install xxx
```

实际上根本不能用。

第二，它已经把 RepoOnboard 最重要的卖点讲清楚了：

> `repoonboard .` → 把陌生代码仓库转成可以理解的项目地图。

第三，等真正做出第一版之后，README 不需要推倒重来。到时候主要替换三块：

```text
Status
↓
Beta / V0.1

Installation
↓
真正的安装命令

顶部
↓
加入最强 Demo GIF / Screenshot
```

尤其以后准备冲 GitHub Star 时，README 最上面应该升级成：

```text
Logo

一句话定位

[Demo GIF]

Installation

repoonboard .

核心功能
```
