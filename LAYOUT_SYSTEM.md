# Form layouts

A layout says **where** a form's fields go. Nothing else.

What a field looks like is decided from its `FieldType`, so a form is the same widget-for-widget
however it happens to be laid out. Whether a field is shown at all is decided by the dependency
engine at fill time, and the validator reads that same answer — so a field can never be demanded and
hidden at once.

A form does not need a layout. Without one its fields are stacked in `orderIndex` order, which is a
perfectly good rendering, so resolution is allowed to answer "none".

## The grid

Positions are expressed on a twelve-column grid, zero-based:

| Field | Meaning |
|---|---|
| `row` | Row position |
| `column` | Column position |
| `colspan` | Columns to span, out of twelve |
| `rowspan` | Rows to span |

A placement with no size falls back to one full-width row. Positions past the twelfth column are
clamped rather than rejected.

**A field the layout says nothing about is still rendered**, appended after the rows the layout does
describe, on an explicit row so grid auto-placement cannot slot it into a hole left on purpose.
Dropping it would leave a required field with no way to answer it and the form permanently
unsubmittable. A layout that covers only some of a form's fields is therefore valid, not a mistake.

## Device types

`MOBILE` and `DESKTOP` — the two the renderer can actually detect. A layout with `deviceType = null`
is the generic one every device falls back to.

Resolution, in order:

1. **Exact match** — a layout stored for the requested device.
2. **Generic** — the layout with `deviceType = null`.
3. **The other device** — laying a form out for the wrong screen beats handing back no layout.
4. **Nothing** — the consumer stacks the fields.

## Authoring

There is no layout editor. Layouts are created over REST or by the demo seeder; the builder
designs a form's fields, not their placement.

## Endpoints

All under `/api/v1/form-layouts`:

| Method | Path | What it does |
|---|---|---|
| `POST` | `/` | Create a layout |
| `PUT` | `/{id}` | Replace a layout's placements |
| `GET` | `/{id}` | One layout by id |
| `GET` | `/form/{formDefinitionId}` | Every layout of a form |
| `GET` | `/form/{formDefinitionId}/device?deviceType=MOBILE` | Exact match only, no fallback |
| `GET` | `/form/{formDefinitionId}/resolve?deviceType=MOBILE` | The fallback chain above; 404 when it ends in nothing |
| `DELETE` | `/{id}` | Delete a layout |

### A two-column desktop layout

```json
{
  "formDefinitionId": 1,
  "deviceType": "DESKTOP",
  "fieldLayouts": [
    { "fieldDefinitionId": 1, "row": 0, "column": 0, "colspan": 6, "rowspan": 1 },
    { "fieldDefinitionId": 2, "row": 0, "column": 6, "colspan": 6, "rowspan": 1 }
  ]
}
```

### A generic fallback layout

```json
{
  "formDefinitionId": 1,
  "deviceType": null,
  "fieldLayouts": [
    { "fieldDefinitionId": 1, "row": 0, "column": 0, "colspan": 12, "rowspan": 1 }
  ]
}
```

## Validation

A layout is refused when the form does not exist, or when it places a field belonging to a different
form — that is a `422` naming each offending field. Covering only part of the form is not a violation.

## Schema

- `form_layouts` — one row per form and device
- `field_layouts` — one row per placed field
