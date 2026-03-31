# Project Glossary and Suggested Data Specification

This document is designed to help you present this app as a professional personal project, even before final real-world datasets are fully available.

## 1) Glossary (business + technical terms)

## GIS (Geographic Information System)
A system used to store, analyze, and visualize geospatial data on maps.

In this project, GIS is used to:
- display water monitoring stations,
- visualize water quality by area,
- run spatial filters (by basin, municipality, radius/buffer),
- generate thematic maps for reporting.

## PostGIS
A PostgreSQL extension that adds spatial data types and geospatial queries.

In this project, PostGIS enables:
- storing station coordinates and geometry,
- spatial indexing for fast map queries,
- operations like distance, containment, and buffer queries.

## Monitoring Point (Station)
A physical location where water samples are collected (e.g., river point, groundwater well, reservoir point).

Typical fields:
- station code,
- name,
- latitude/longitude,
- water type,
- basin/municipality.

## Parameter
A measured water-quality indicator (e.g., pH, nitrate, conductivity, dissolved oxygen).

Typical fields:
- parameter code,
- display label,
- unit,
- valid min/max range.

## Campaign
A sampling event or time window (e.g., Spring 2026 campaign).

Typical fields:
- campaign id,
- date range,
- season,
- source (lab/field).

## Measurement
One observed value for one parameter at one monitoring point and one time.

Example:
- Point MYA-042,
- Parameter Nitrate,
- Value = 12.4,
- Unit = mg/L,
- Date = 2026-03-21.

## Norm / Threshold
Reference limits used to classify or validate quality values.

Example:
- nitrate upper limit = 50 mg/L,
- dissolved oxygen minimum = 5 mg/L.

## Audit Trail
A chronological log of user actions (who did what and when), especially for sensitive operations.

Examples:
- role changes,
- user deletion,
- data import,
- manual measurement edits.

## RBAC (Role-Based Access Control)
Security model where permissions are assigned by role (e.g., SADMIN, ADMIN, TECHNICIAN, VIEWER/USER).

---

## 2) Suggested Domain Data Structure (MVP)

Below is a practical structure aligned with the TDR and your app direction.

## Core entities

### `users`
- `id` (bigint, PK)
- `username` (varchar)
- `email` (varchar, unique)
- `password_hash` (varchar)
- `role` (enum: USER, TECHNICIAN, ADMIN, SADMIN)
- `created_at` (timestamp)
- `updated_at` (timestamp)

### `monitoring_points`
- `id` (bigint, PK)
- `code` (varchar, unique) — e.g., MYA-042
- `name` (varchar)
- `water_type` (enum: GROUNDWATER, SURFACE, RESERVOIR)
- `basin` (varchar)
- `municipality` (varchar)
- `latitude` (numeric)
- `longitude` (numeric)
- `geom` (geometry(Point, 4326))
- `active` (boolean)

### `parameters`
- `id` (bigint, PK)
- `code` (varchar, unique) — e.g., PH, NO3, EC
- `label` (varchar)
- `unit` (varchar)
- `normal_min` (numeric, nullable)
- `normal_max` (numeric, nullable)
- `category` (varchar) — physico-chemical, microbiological, etc.

### `campaigns`
- `id` (bigint, PK)
- `name` (varchar) — e.g., Spring 2026
- `start_date` (date)
- `end_date` (date)
- `season` (varchar)
- `source` (varchar)
- `status` (enum: DRAFT, VALIDATED, ARCHIVED)

### `measurements`
- `id` (bigint, PK)
- `point_id` (FK -> monitoring_points.id)
- `parameter_id` (FK -> parameters.id)
- `campaign_id` (FK -> campaigns.id, nullable)
- `sampled_at` (timestamp)
- `value` (numeric)
- `unit` (varchar)
- `quality_flag` (enum: OK, WARNING, OUTLIER, INVALID)
- `lab_reference` (varchar, nullable)
- `created_by` (FK -> users.id)
- `created_at` (timestamp)

### `norm_thresholds`
- `id` (bigint, PK)
- `parameter_id` (FK -> parameters.id)
- `water_type` (enum)
- `lower_limit` (numeric, nullable)
- `upper_limit` (numeric, nullable)
- `severity` (enum: INFO, WARNING, CRITICAL)
- `version` (varchar) — e.g., MOROCCO_NORM_2024

### `audit_logs`
- `id` (bigint, PK)
- `actor_user_id` (FK -> users.id)
- `action` (varchar) — ROLE_CHANGE, USER_DELETE, IMPORT_RUN, etc.
- `entity_type` (varchar)
- `entity_id` (varchar)
- `before_json` (jsonb, nullable)
- `after_json` (jsonb, nullable)
- `created_at` (timestamp)

---

## 3) Suggested import file format (CSV)

Use this as a standard template for field/lab uploads.

```csv
campaign_name,point_code,sampled_at,parameter_code,value,unit,lab_reference,source_file_row
Spring 2026,MYA-042,2026-03-21T10:30:00Z,PH,7.40,pH,LAB-001,2
Spring 2026,MYA-042,2026-03-21T10:30:00Z,NO3,12.40,mg/L,LAB-001,3
Spring 2026,MYA-087,2026-03-22T09:00:00Z,EC,450,uS/cm,LAB-002,2
```

## Validation rules (minimum)
- `point_code` must exist.
- `parameter_code` must exist.
- `sampled_at` must be a valid date-time.
- `value` must be numeric.
- `unit` must match parameter unit or valid conversion rule.
- Reject duplicates by (`point_code`, `parameter_code`, `sampled_at`, `campaign_name`).
- Attach row-level errors in import report.

---

## 4) Suggested API contract (MVP)

## Auth
- `POST /auth/login`
- `POST /auth/register`

## Admin user management
- `GET /admin/users`
- `PATCH /admin/users/{id}/role`
- `DELETE /admin/users/{id}`

## Monitoring points
- `GET /points`
- `POST /points`
- `PATCH /points/{id}`
- `DELETE /points/{id}`

## Parameters
- `GET /parameters`
- `POST /parameters`
- `PATCH /parameters/{id}`

## Campaigns
- `GET /campaigns`
- `POST /campaigns`

## Measurements
- `GET /measurements?pointCode=&parameterCode=&from=&to=&campaignId=`
- `POST /measurements`

## Imports
- `POST /imports` (multipart file upload)
- `GET /imports/{id}` (status/report)

## Dashboard analytics
- `GET /dashboard/overview`
- `GET /dashboard/timeseries?parameterCode=&pointCode=&from=&to=`
- `GET /dashboard/alerts?severity=`

## GIS
- `GET /gis/points`
- `GET /gis/heatmap?parameterCode=&campaignId=`
- `GET /gis/query?basin=&municipality=&buffer=`

---

## 5) Suggested role matrix (presentation-ready)

- **SADMIN**
  - Full system control.
  - Can assign any role.
  - Can remove ADMIN/TECHNICIAN/USER (but not another SADMIN, if you choose that policy).
- **ADMIN**
  - Operational administration.
  - Can assign only lower roles (TECHNICIAN, USER).
  - Can remove TECHNICIAN/USER.
- **TECHNICIAN**
  - Data import, data entry, updates.
  - Cannot manage users/roles.
- **VIEWER/USER**
  - Read-only dashboards/maps/reports.

---

## 6) How to present this project to clients

When presenting, position it as:
- **Problem solved**: fragmented water-quality data, low traceability, limited reporting speed.
- **Solution**: centralized data platform + validation + GIS + analytics + governance.
- **Business value**:
  - faster reporting,
  - better data quality,
  - map-driven decision making,
  - full accountability via audit logs.

A short pitch you can reuse:

> “I built a water-quality data management platform that centralizes monitoring data, validates imports, visualizes trends and geospatial patterns, and enforces role-based governance. It is designed for agencies managing groundwater, surface water, and reservoir quality at scale.”

---

## 7) Next practical step

If you want, the next file I can generate is:
- `docs/sample-data-dictionary.md` with field-by-field descriptions (type, unit, required, validation, business meaning),
so you can show a more formal technical package during client demos.
