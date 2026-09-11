---
name: RepoOnboard Core
colors:
  surface: '#f9f9ff'
  surface-dim: '#d7dae5'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f1f3ff'
  surface-container: '#ebedfa'
  surface-container-high: '#e5e8f4'
  surface-container-highest: '#dfe2ee'
  on-surface: '#181c24'
  on-surface-variant: '#3e484f'
  inverse-surface: '#2c3039'
  inverse-on-surface: '#eef0fc'
  outline: '#6e7980'
  outline-variant: '#bdc8d1'
  surface-tint: '#00668a'
  primary: '#00668a'
  on-primary: '#ffffff'
  primary-container: '#38bdf8'
  on-primary-container: '#004965'
  inverse-primary: '#7bd0ff'
  secondary: '#006591'
  on-secondary: '#ffffff'
  secondary-container: '#39b8fd'
  on-secondary-container: '#004666'
  tertiary: '#855300'
  on-tertiary: '#ffffff'
  tertiary-container: '#f1a02b'
  on-tertiary-container: '#613b00'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c4e7ff'
  primary-fixed-dim: '#7bd0ff'
  on-primary-fixed: '#001e2c'
  on-primary-fixed-variant: '#004c69'
  secondary-fixed: '#c9e6ff'
  secondary-fixed-dim: '#89ceff'
  on-secondary-fixed: '#001e2f'
  on-secondary-fixed-variant: '#004c6e'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb960'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#f9f9ff'
  on-background: '#181c24'
  surface-variant: '#dfe2ee'
typography:
  headline-lg:
    fontFamily: Geist
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Geist
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0em
  body-sm:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0em
  mono-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: -0.02em
  mono-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: -0.02em
  label-sm:
    fontFamily: Geist
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.02em
  code-badge:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 12px
    letterSpacing: 0.04em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  space-2xs: 2px
  space-xs: 4px
  space-sm: 8px
  space-md: 12px
  space-lg: 16px
  space-xl: 20px
  space-2xl: 24px
  space-3xl: 32px
  sidebar-width: 240px
  inspector-width: 340px
  header-height: 44px
---

## Brand & Style

### Personality & Core Pillars
The design system delivers a quiet, technical, and deterministic workspace tailored for software engineers navigating complex, unfamiliar codebases. Inspired by the rigor of JetBrains IDEs, the visual discipline of Linear, and the utilitarian clarity of GitHub and Vercel infrastructure tooling, the system acts as an analytical instrument rather than a consumer dashboard.

- **Evidence over Inference:** Every visual element represents a confirmed fact (file path, AST symbol, dependency edge, HTTP verb). No speculative scores, AI sparkles, or non-deterministic decorative filler.
- **Quiet Precision:** High-density, low-fatigue light interfaces engineered for continuous hours of technical investigation.
- **Instrument Density:** Information density takes precedence over decorative breathing room. Compact layouts, micro-padding, explicit hairline dividers, and monospace metadata form the foundation.

### Style Archetype: Precision Technical Minimalism
The visual execution relies on flat, border-governed architecture. Drop shadows, decorative gradients, glassmorphic blurs, and organic roundness are rejected. Visual structure is maintained strictly through subtle slate-tinted hair lines, deliberate surface elevation tiers, and precise typographic hierarchy.

## Colors

### Light-First Architecture
The system is configured for a light color mode while retaining high-contrast, clean technical boundaries. Surface contrast operates through tight tonal shifts:

- **Canvas (`#0B0F17`):** The foundational substrate for graph viewports, underlays, and base window chrome.
- **Surface 1 (`#111726`):** The primary container background for structural rails, sidebars, panel bodies, and content sections.
- **Surface 2 (`#182234`):** Secondary elevated containers, table row highlights, active card bodies, and interactive form controls.
- **Overlay (`#1E293B`):** Context menus, command palettes, floating popovers, and inspector tooltips.
- **Borders (`#1F2E45` & `#273752`):** Crisp 1px borders that substitute for elevation shadows across the entire UI.

### Interactive Accent
- **Primary Accent (`#38BDF8`):** Low-saturation technical sky cyan. Reserved for primary actions, active navigation anchors, graph selection rings, and keyboard focus states.
- **Secondary Accent (`#0EA5E9`):** Deeper technical blue used for hover states on primary interactive elements and active indicator tabs.

### Semantic Fact Rules
Semantic colors convey deterministic analyzer findings. Color is never used alone; each state is accompanied by a symbol, badge text, or path notation:
- **Success (`#22C55E`):** Confirmed, verified, and parsed dependencies or tests.
- **Warning (`#F59E0B`):** Partial, incomplete AST analysis, circular dependencies, or deprecated symbols.
- **Error (`#EF4444`):** Analysis failure, unparseable source files, or broken references.
- **Unresolved (`#A855F7`):** Explicit ambiguous identifiers or unknown types (never rendered as empty or zero).

## Typography

### Typographic Split
The typographic system enforces a strict division between interface labels and code-derived symbols:

1. **Interface Typography (Geist):** Clean, neutral sans-serif designed for high legibility in dense desktop interfaces. Used for panel titles, navigation labels, button text, explanations, and instructions.
2. **Code & Technical Typography (JetBrains Mono):** Monospaced type used for file paths (`src/main/java`), Maven coordinates (`groupId:artifactId`), Spring bean identifiers, REST methods (`GET`, `POST`), line/column numbers, and stack traces.

### Scale Rules
- **No Marketing Scales:** Large hero titles (>24px) are prohibited. Top-level workbench titles cap at 20px.
- **Information Density:** Body and monospace tokens default to 11–13px to maximize visible context within 3-pane workspaces.
- **Numbers and Alignments:** Monospace fonts are enforced across all numerical metrics, timestamps, and row counts to preserve tabular alignment across lists and inspector panes.

## Layout & Spacing

### 3-Pane Workbench Architecture
The layout uses a fixed 3-pane workstation model engineered for IDE-style investigation:

1. **Primary Navigation Rail / Left Sidebar (Fixed 240px):** File tree, repository structural index, module selector, and architectural perspectives.
2. **Contextual Workspace (Flexible / Center Canvas):** The central stage displaying the graph canvas (Cytoscape.js), deterministic dependency matrices, API route tables, or structural code outlines.
3. **Inspector Panel (Fixed 340px, Collapsible):** Persistent right-hand pane detailing the currently focused node, bean, endpoint, or class, including source location, callers, callees, and verified Maven coordinates.

### Spacing Rhythm
- Built on a strict 4px grid (`4px`, `8px`, `12px`, `16px`, `20px`, `24px`).
- Dense metadata lists utilize compact 24px–28px row heights with 8px horizontal padding.
- Panel headers maintain a consistent 44px vertical height to align across all three columns horizontally.

### Breakpoints & Responsive Behavior
- **Desktop (>= 1280px):** Full 3-column layout visible. Inspector panel pinned open.
- **Compact Window (960px - 1279px):** Inspector transitions to a collapsible slide-over drawer anchored to the right border.
- **Narrow / Split Window (< 960px):** Graph visualizations degrade automatically into searchable tabular lists; side navigation collapses into an icon rail or command-palette selector.

## Elevation & Depth

### Flat, Border-Governed Depth
The design system avoids ambient drop shadows, blurred light reflections, or layered skeuomorphic bevels. Visual separation is accomplished through:

1. **Tonal Surface Hierarchy:**
   - Deepest: `Canvas` (`#0B0F17`)
   - Structural Panels: `Surface 1` (`#111726`)
   - Nested Containers & Inputs: `Surface 2` (`#182234`)
   - Flyouts & Menus: `Overlay` (`#1E293B`)
2. **Hairline Seams:** Continuous 1px borders (`#1F2E45` or `#273752`) define boundaries between workspace panels, splitters, table headers, and selected cards.
3. **Restrained Overlay Shadows:** Floating components (command palettes, dropdown menus, context tooltips) utilize a single sharp, non-diffuse elevation style:
   - `box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.45), 0 0 0 1px #273752;`
4. **Graph Canvas Depth:** Nodes sit flush against the `#0B0F17` canvas. Active selection is communicated solely through a 1px border stroke in `#38BDF8`, never through glowing halos or neon radial spreads.

## Shapes

### Restrained Industrial Geometry
The shape language enforces a compact, engineered feel (`roundedness: 1`):

- **Inputs, Buttons, Badges, Tabs:** 4px radius (`0.25rem`). Maintains visual firmness and maximizes internal padding efficiency.
- **Workspace Cards, Panels, Flyouts:** 6px radius (`0.375rem`).
- **Graph Nodes:** 4px radius rectangle containers. Round circles are reserved strictly for status dots and indicator icons.
- **Prohibited:** Full pill shapes (`9999px` border-radius), bubbly consumer buttons, and oversized card corners (>8px) are disallowed.

## Components

### 1. Buttons
- **Primary:** Background `#38BDF8`, text `#0B0F17`, font-weight 500, height 28px (compact) or 32px (standard), border-radius 4px. Hover: `#0EA5E9`.
- **Secondary / Outline:** Background `transparent`, border 1px solid `#273752`, text `#E2E8F0`. Hover: background `#182234`, border-color `#38BDF8`.
- **Ghost / Tool:** Background `transparent`, border none, text `#94A3B8`. Hover: background `#182234`, text `#E2E8F0`.
- **Destructive:** Background `transparent`, border 1px solid rgba(239, 68, 68, 0.4), text `#EF4444`. Hover: background rgba(239, 68, 68, 0.1).

### 2. Badges & Chips
- **Monochromatic Fact Badge:** Background `#182234`, border 1px solid `#273752`, text `#94A3B8`, JetBrains Mono, 10px font size, uppercase.
- **HTTP Method Badges:**
  - `GET`: Background `#0C2D3A`, text `#38BDF8`, border 1px solid `#0369A1`.
  - `POST`: Background `#0F2E1E`, text `#4ADE80`, border 1px solid `#15803D`.
  - `PUT`/`PATCH`: Background `#2E220F`, text `#FBBF24`, border 1px solid `#B45309`.
  - `DELETE`: Background `#321415`, text `#F87171`, border 1px solid `#B91C1C`.
- **Status Badges:** Small 6px solid dot indicator + sans-serif text. Never color-only.

### 3. Data Tables & Tree Lists
- **Rows:** Alternating subtle or unified `#111726` background, 28px height, 1px bottom border `#1F2E45`.
- **Tree Guides:** 1px vertical hairline (`#1F2E45`) with 12px indentation increments.
- **Hover/Selected:** Hover produces `#182234`; active selection applies a 2px left border accent `#38BDF8` with `#182234` fill.

### 4. Input Fields & Search Bars
- **Style:** Background `#0B0F17`, border 1px solid `#1F2E45`, text `#E2E8F0`, font-size 12px, height 30px, border-radius 4px.
- **Focus:** Border `#38BDF8`, zero outer ring shadow (`outline: none`).
- **Keyboard Shortcuts:** Inline monochromatic `kbd` elements (`#182234` background, `#64748B` text, 1px border `#273752`).

### 5. Inspector Panels
- **Structure:** Split into Collapsible Sections:
  - Header: Entity Title (Geist 14px) + FQN (JetBrains Mono 11px, `#94A3B8`).
  - Evidence List: Key-value rows with label in `#64748B` (sans-serif) and value in `#E2E8F0` (JetBrains Mono).
  - Quick Actions: One-click "Copy Path", "Open in IDE", "Jump to Symbol".

### 6. Cytoscape Graph Nodes
- **Geometry:** Bounded box (minimum width 120px, height 36px), 4px radius, filled with `#111726`, border 1px solid `#1F2E45`.
- **Typography:** Entity Name in 11px Geist, Sub-label (stereotype/package) in 9px JetBrains Mono.
- **Edges:**
  - Maven Dependency: Solid 1px line `#273752`.
  - Spring Runtime Wire: Dashed 1px line `#38BDF8`.
  - Unresolved Reference: Dotted 1px line `#A855F7`.