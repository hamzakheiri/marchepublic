# Implementation Roadmap (based on `Extrait tdr.pdf`)

## 1) What the TDR requires (practical summary)

Based on the TDR extract (Chapter II), the solution should cover:
- **Mission I**: collect, clean, harmonize, and integrate historical data into a relational database (PostgreSQL/PostGIS), including a data dictionary.
- **Mission II**: web application + GIS with:
  - authentication and hierarchical roles,
  - data entry and update,
  - batch Excel/CSV import,
  - automatic validation,
  - consultation and filtering,
  - statistical analysis and charts,
  - threshold exceedance alerts,
  - interactive cartography,
  - export and reporting,
  - system administration (parameters, standards, points, backups).
- **Mission III**: training, manuals, and knowledge transfer.

## 2) Current state observed in the app

- **Already in place**
  - Basic authentication and roles in the app.
  - Admin user management (list / role change / delete) in progress.
  - Structured main UI pages (`Dashboard`, `Import`, `Map`).
- **Major gaps vs TDR**
  - Core water-quality domain model (points, parameters, campaigns, analyses, standards).
  - Real CSV/Excel import with a validation pipeline.
  - Analytical dashboard connected to real data.
  - Real GIS implementation (PostGIS layers, spatial filters, queries).
  - Automated exports and reports.
  - Audit trail logging and manageable backups.

## 3) Recommended implementation order (next priorities)

## Phase A — Data foundations (Mission I first)
1. **Core data model**
   - Create minimal entities:
     - `MonitoringPoint` (station, coordinates, basin/municipality, water type),
     - `Parameter` (code, unit, range),
     - `Campaign` (date, season, source),
     - `Measurement` (value, point, parameter, date),
     - `NormThreshold` (threshold per parameter/type).
2. **Data dictionary**
   - Document types, units, constraints, and relationships.
3. **SQL/Postgres migrations**
   - Set up versioned migrations (Flyway recommended).

**Concrete deliverable**: stable SQL schema + minimal CRUD API for points/parameters/campaigns/measurements.

## Phase B — Import and data quality (Mission II critical block)
1. **Real CSV/Excel import**
   - Endpoint `POST /imports` (file + campaign metadata).
2. **Automatic validation**
   - Checks: format, units, duplicates, normal ranges, point/parameter/date consistency.
3. **Import report**
   - Summary: accepted/rejected rows + downloadable detailed errors.
4. **Import UI connected to API**
   - Replace the `ImportPage` mock with upload + progress + error feedback.

**Concrete deliverable**: one field file imported end-to-end with full reject traceability.

## Phase C — Consultation and analytical dashboard
1. **Multi-criteria search API**
   - Filters: date/campaign/parameter/point/basin.
2. **Real dashboard widgets**
   - Replace static numbers with backend aggregations.
3. **Time-series and comparison charts**
   - Parameter time series + campaign/season comparisons.
4. **Standards exceedance alerts**
   - Normal/alert status based on `NormThreshold`.

**Concrete deliverable**: operational dashboard powered by imported measurements.

## Phase D — Operational GIS
1. **Real map integration**
   - Leaflet/MapLibre + monitoring-point layers.
2. **Thematic mapping**
   - Color/size by concentration.
3. **Spatial queries**
   - Buffer and basin/municipality filtering.
4. **Map export**
   - PNG/PDF export of thematic views.

**Concrete deliverable**: non-mock `MapPage` with business filters and PostGIS-backed results.

## Phase E — Governance, security, operations
1. **Hardened backend RBAC**
   - Move to robust auth (JWT/server session) instead of trusting simple headers.
2. **Complete audit trail**
   - Logins + sensitive actions (import, delete, role change, measurement edits).
3. **Backups**
   - Manual and scheduled backup process/endpoints.
4. **Exports and reports**
   - PDF/Excel by point/campaign/parameter.

---

## 4) Recommended short-term MVP backlog

### Sprint 1 (2 weeks)
- Domain schema + migrations.
- CRUD for points/parameters/campaigns.
- Basic CSV import + minimal validation.

### Sprint 2 (2 weeks)
- API-connected dashboard (core stats + key charts).
- Multi-criteria consultation.
- Standards exceedance alerts.

### Sprint 3 (2 weeks)
- Real map (points + simple filters).
- Table exports (CSV/PDF).
- Minimal audit trail.

## 5) Validation criteria (aligned with TDR)

- Real file import with quality checks and error reporting.
- Performant filtered consultation (acceptable response time).
- Interactive mapping of monitoring points.
- Correct role enforcement (admin/technician/viewer + agreed internal hierarchy).
- Updated technical documentation and data dictionary.

## 6) Decisions to make now

1. **Immediate business priority**: groundwater, surface water, or reservoirs first?
2. **Standards reference**: official source + initial version.
3. **Standard import format**: define one approved CSV/Excel template for field teams.
4. **Frontend map stack choice**: Leaflet (simpler) vs MapLibre (more advanced).

---

If you want, I can start directly with **Phase A** and generate the first technical batch:
- JPA entity model,
- SQL migrations,
- minimal CRUD endpoints,
- import CSV template.
