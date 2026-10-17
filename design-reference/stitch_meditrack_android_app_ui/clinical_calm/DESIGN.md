---
name: Clinical Calm
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#3d4947'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#6d7a77'
  outline-variant: '#bcc9c6'
  surface-tint: '#006a61'
  primary: '#00685f'
  on-primary: '#ffffff'
  primary-container: '#008378'
  on-primary-container: '#f4fffc'
  inverse-primary: '#6bd8cb'
  secondary: '#006398'
  on-secondary: '#ffffff'
  secondary-container: '#5bb8fe'
  on-secondary-container: '#00476e'
  tertiary: '#006b2c'
  on-tertiary: '#ffffff'
  tertiary-container: '#00873a'
  on-tertiary-container: '#f7fff2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#89f5e7'
  primary-fixed-dim: '#6bd8cb'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#005049'
  secondary-fixed: '#cce5ff'
  secondary-fixed-dim: '#93ccff'
  on-secondary-fixed: '#001d31'
  on-secondary-fixed-variant: '#004b73'
  tertiary-fixed: '#7ffc97'
  tertiary-fixed-dim: '#62df7d'
  on-tertiary-fixed: '#002109'
  on-tertiary-fixed-variant: '#005320'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system delivers an authoritative, reassuring, and distraction-free experience for offline-first medical adherence. Built upon Material Design 3 principles tailored for healthcare, the interface prioritizes immediate legibility, high motor-accessibility, and unambiguous cognitive feedback. 

Patients and caregivers rely on this application in high-stress, routine, or visually impaired conditions. The emotional tone must remain calm, clinically precise, and deeply dependable—avoiding aggressive alerts or sensory overload while maintaining strict clarity regarding medication states.

The design movement is **Modern / Tactile-Material**: structured structural surfaces, crisp low-contrast borders, purposeful state-driven tinting, and generous touch affordances calibrated for rapid thumb interactions on mobile devices.

## Colors

The palette balances clinical precision with reassuring warmth. Light mode is the default state to maximize outdoor readability and daytime clarity, anchored by soft off-white canvas foundations rather than stark blue-white.

### Palette Architecture
- **Primary (`#0D9488`)**: Calm teal used for dominant interactive triggers, active navigation states, and confirmed actions. Dark variant `#0F766E` applies to pressed states and primary typography accents; light variant `#14B8A6` serves interactive highlights.
- **Secondary (`#0284C7`)**: Slate cyan handling supplemental workflows, schedule timelines, and info containers. Paired with `#E0F2FE` for subtle tonal selections.
- **Neutral Canvas (`#F8FAFC`)**: Soft background off-white reducing ocular strain. Containers shift to `#FFFFFF` with `#E2E8F0` structural outlines.
- **Neutral Secondary (`#F1F5F9`)**: Neutral fill used for unselected chips, disabled controls, and pending tracks.

### Semantic Status Tiers
Adherence requires unambiguous state communication via paired foregrounds and tinted surface containers:
- **Confirmed Taken**: `#16A34A` text/icon on `#DCFCE7` surface. Conveys health success and verified intake.
- **User Skipped**: `#DC2626` text/icon on `#FEE2E2` surface. Conveys intentional omission without punitive visual alarms.
- **Attention / Late / Due**: `#D97706` text/icon on `#FEF3C7` surface. Demands timely action before window expiry.
- **Pending / Upcoming**: `#64748B` text/icon on `#F1F5F9` surface. Passive future intake states.

## Typography

The typographic hierarchy relies on **Inter** across all roles to ensure universal rendering, deep system compatibility, and robust character sets. 

### Tabular Timing Feature
All digital timestamps, countdown clocks, dosages (e.g., `500 mg`, `08:30 AM`), and log intervals must enforce tabular figures using CSS `font-variant-numeric: tabular-nums` or OpenType feature tag `tnum`. This prevents structural horizontal jitter across active tracking cards and schedule lists.

### Hierarchy & Legibility Rules
- **Display & Headlines**: Clear, compact, and confident. `headline-lg-mobile` anchors primary overview headers, keeping focal points high on small viewport handhelds.
- **Body Content**: Tuned with relaxed line-heights to support users with low vision or reading fatigue.
- **Labels & Microcopy**: Form labels and status pills use medium-to-semibold weights with micro-letter spacing to preserve definition against tinted surface fills.

## Layout & Spacing

Layout geometry follows an 8dp structural grid with a 4dp baseline sub-grid. Handheld ergonomics govern all spatial relationships.

### Touch Boundaries & Margins
- **Touch Targets**: All interactive elements (pills, menu triggers, dose action confirmers, navigation items) enforce a hard minimum bounding box of 48dp × 48dp, regardless of optical visual boundaries.
- **Canvas Margins**: Set to `1rem` (16dp) on mobile devices, establishing a continuous vertical rail aligned with Android system UI bars.
- **Gutter Rhythm**: Card and list separators use `space-md` (16dp) for discrete medication modules and `space-sm` (8dp) between related sub-entries.

### Screen Adaptability
- **Mobile (<600dp)**: Single column with persistent bottom navigation bar. Stacked schedule feed with full-width card structures.
- **Tablet / Foldable (600dp–840dp)**: Adaptive two-column split screen: master schedule listing anchored on the left rail, live dose confirmation and detail inspector docked on the right rail. Bottom navigation shifts to a compact vertical navigation rail on the left edge.

## Elevation & Depth

Visual hierarchy is maintained via an ambient, layered Material 3 surface system that replaces heavy drop shadows with soft depth planes and tonal border grounding.

### Surface Tiers
- **Level 0 (Background Canvas)**: `#F8FAFC`. Base canvas behind scrollable feeds.
- **Level 1 (Default Containers & Cards)**: `#FFFFFF` fill bounded by a crisp 1px solid `#E2E8F0` border. Shadow: `0px 1px 3px rgba(15, 23, 42, 0.04), 0px 1px 2px rgba(15, 23, 42, 0.02)`.
- **Level 2 (Active Dose & Elevated Modals)**: `#FFFFFF` fill with border `#CBD5E1`. Shadow: `0px 4px 6px -1px rgba(13, 148, 136, 0.06), 0px 2px 4px -2px rgba(15, 23, 42, 0.05)`. Subtle teal-tinted ambient scattering indicates contextual urgency.
- **Level 3 (Dialogs & Floating Overlays)**: `#FFFFFF` fill, 0px border. Shadow: `0px 10px 15px -3px rgba(15, 23, 42, 0.08), 0px 4px 6px -4px rgba(15, 23, 42, 0.04)`.

Scrolled top app bars adopt Level 1 surface treatment with an under-border of `#E2E8F0` rather than aggressive drop shadows.

## Shapes

The interface embraces a humanized, medical-grade geometry balancing ergonomic softness with structured order.

- **Primary Geometry (16dp / `rounded-lg`)**: Applied across dose schedule cards, action modals, text entry fields, and bottom sheet containers. This creates a soft, approachable feel suited for daily health tasks.
- **Secondary Geometry (8dp / `rounded-md`)**: Reserved for micro-components including status chips, pill tags, dose indicators, and segmented filter controls.
- **Full Curvature (9999dp / Pill)**: Reserved exclusively for bottom navigation active item indicators and primary action buttons (FABs / quick dose confirmation triggers).

## Components

### Buttons
- **Primary ("Log Dose")**: Height 48dp, 16dp corner radius. Fill `#0D9488`, label `#FFFFFF` in `label-lg`. Pressed state transitions to `#0F766E`.
- **Secondary / Outlined**: Height 48dp, 16dp corner radius. Transparent fill with 1.5px border `#0D9488`, label `#0D9488`.
- **Tertiary / Destructive ("Skip Dose")**: Height 48dp, 16dp corner radius. Fill `#FEE2E2`, label `#DC2626`. Pressed state shifts to `#FECACA`.

### Status Chips & Badges
- 8dp corner radius with 6dp horizontal padding, 4dp vertical padding.
- Employs strict semantic tint pairs:
  - *Taken*: `#DCFCE7` surface with `#16A34A` label and checkmark icon.
  - *Skipped*: `#FEE2E2` surface with `#DC2626` label and forward-arrow icon.
  - *Due / Late*: `#FEF3C7` surface with `#D97706` label and clock icon.
  - *Upcoming*: `#F1F5F9` surface with `#64748B` label.

### Cards (Dose & Medication Card)
- 16dp corner radius, `#FFFFFF` background, 1px `#E2E8F0` stroke. Internal padding: `1rem` (16dp).
- **Structure**:
  - *Top Row*: Scheduled intake time (`tabular-nums`, `headline-md`), accompanied by status chip docked at the top-right.
  - *Middle Row*: Medication brand name (`headline-sm`), strength and form dosage (`body-md` in `#64748B`), special instructions (e.g., "Take with food").
  - *Bottom Action Row*: Quick-action buttons maintaining minimum 48dp touch heights, separated by `space-sm` (8dp).

### Input Fields
- Height 56dp (standard Material touch rail).
- 14dp–16dp corner radius. `#FFFFFF` background, 1.5px `#CBD5E1` border resting; transitions to 2px `#0D9488` upon focus.
- Floating label typography `label-md` seated above user text in `body-lg`. Offline cached indicators render in `label-sm` with slate `#64748B`.

### Checkboxes & Radio Controls
- Minimum 48dp target bounding frame with an internal 20dp selection box.
- Checked state: Fill `#0D9488` with crisp `#FFFFFF` check glyph.
- Unchecked state: Transparent fill with 2px stroke `#94A3B8`.

### Navigation Architecture
- **Persistent Bottom Navigation**: Fixed 80dp height container, `#FFFFFF` surface with 1px top stroke `#E2E8F0`. Four destinations: *Schedules*, *Dose*, *Reports*, *Caregiver*.
- **Active Destination Indicator**: 64dp × 32dp pill-shaped highlight in `#CCFBF1` (teal tint) enclosing an `#0F766E` icon, accompanied by `label-sm` label in `#0F766E`. Inactive items display in `#64748B`.

### Domain-Specific Components
- **Offline Sync Banner**: Low-profile `#0284C7` (slate cyan) status ribbon across view top indicating local sqlite state and queued synchronization items without disrupting the workflow.
- **Pill Identifier Pillbox**: Visual representation box within the card highlighting shape, color, and debossed imprint text of the physical pill to prevent medication errors.