# Form Layout System - Responsive Design Implementation

## Overview

This implementation provides a flexible, responsive layout system for forms with inheritance and fallback strategies.

## Key Features

### 1. Device-Specific Layouts
Forms can have multiple layouts optimized for different device types:
- **MOBILE**: Optimized for smartphones
- **TABLET**: Optimized for tablets
- **DESKTOP**: Optimized for desktop browsers

### 2. Inheritance Strategy
The system supports a fallback hierarchy:

1. **Exact Match**: If a layout exists for the requested device type, it will be used
2. **Generic Fallback**: If no device-specific layout exists, the system uses a layout with `deviceType = null` (generic/default layout)
3. **Closest Match**: If neither exists, the system finds the closest matching device type:
   - TABLET → DESKTOP → MOBILE
   - MOBILE → TABLET
   - DESKTOP → TABLET

### 3. Internal Forms Support
Forms designed for internal/programmatic use may not have layouts defined. The system handles this gracefully:
- A warning is logged when layouts are missing fields
- The system can generate default layouts automatically
- Not all fields need to be included in a layout (useful for API-only forms)

### 4. Custom Component Types
Each field in a layout can have a custom component type, allowing frontend flexibility:
- Default component types: TEXT_INPUT, NUMBER_INPUT, DATE_PICKER, SELECT, CHECKBOX, RADIO, TEXTAREA, FILE_UPLOAD
- CUSTOM type with configurable properties stored in `customProperties` map

### 5. Grid-Based Positioning
Layouts use a grid system with:
- `row`: Row position
- `column`: Column position
- `colspan`: Number of columns to span (default 12-column grid)
- `rowspan`: Number of rows to span
- `visible`: Toggle field visibility

## API Endpoints

### Create Layout
```
POST /api/v1/form-layouts
```

### Update Layout
```
PUT /api/v1/form-layouts/{id}
```

### Get Layout by ID
```
GET /api/v1/form-layouts/{id}
```

### Get All Layouts for a Form
```
GET /api/v1/form-layouts/form/{formDefinitionId}
```

### Get Layout by Device Type
```
GET /api/v1/form-layouts/form/{formDefinitionId}/device?deviceType=MOBILE
```

### Resolve Layout (with fallback logic)
```
GET /api/v1/form-layouts/form/{formDefinitionId}/resolve?deviceType=MOBILE
```

### Resolve Layout or Generate Default
```
GET /api/v1/form-layouts/form/{formDefinitionId}/resolve-or-default?deviceType=MOBILE
```

### Delete Layout
```
DELETE /api/v1/form-layouts/{id}
```

## Example Usage

### Creating a Mobile Layout
```json
{
  "formDefinitionId": 1,
  "deviceType": "MOBILE",
  "fieldLayouts": [
    {
      "fieldDefinitionId": 1,
      "row": 0,
      "column": 0,
      "colspan": 12,
      "rowspan": 1,
      "componentType": "TEXT_INPUT",
      "visible": true
    },
    {
      "fieldDefinitionId": 2,
      "row": 1,
      "column": 0,
      "colspan": 12,
      "rowspan": 1,
      "componentType": "NUMBER_INPUT",
      "visible": true
    }
  ]
}
```

### Creating a Desktop Layout (2-column)
```json
{
  "formDefinitionId": 1,
  "deviceType": "DESKTOP",
  "fieldLayouts": [
    {
      "fieldDefinitionId": 1,
      "row": 0,
      "column": 0,
      "colspan": 6,
      "rowspan": 1,
      "componentType": "TEXT_INPUT",
      "visible": true
    },
    {
      "fieldDefinitionId": 2,
      "row": 0,
      "column": 6,
      "colspan": 6,
      "rowspan": 1,
      "componentType": "NUMBER_INPUT",
      "visible": true
    }
  ]
}
```

### Creating a Generic/Fallback Layout
```json
{
  "formDefinitionId": 1,
  "deviceType": null,
  "fieldLayouts": [
    {
      "fieldDefinitionId": 1,
      "row": 0,
      "column": 0,
      "colspan": 12,
      "rowspan": 1,
      "visible": true
    }
  ]
}
```

### Using Custom Component with Properties
```json
{
  "fieldDefinitionId": 1,
  "row": 0,
  "column": 0,
  "colspan": 12,
  "rowspan": 1,
  "componentType": "CUSTOM",
  "customProperties": {
    "componentName": "RichTextEditor",
    "toolbar": ["bold", "italic", "underline"],
    "maxHeight": "300px"
  },
  "visible": true
}
```

## Database Schema

### Tables Created
- `form_layouts`: Stores layout definitions for forms
- `field_layouts`: Stores individual field positioning and properties
- `field_layout_properties`: Stores custom properties for field layouts

## Validation

The system validates:
1. Form definition exists before creating a layout
2. All field IDs in the layout belong to the form definition
3. Invalid field IDs are rejected
4. Missing fields trigger a warning (but don't block creation)

## Frontend Integration

The frontend (Vaadin) should:
1. Detect device type or receive it from user
2. Call `/resolve-or-default` endpoint with device type
3. Render form based on returned layout (row, column, colspan, rowspan)
4. Use `componentType` to determine which component to render
5. Apply `customProperties` to components as needed
6. Respect `visible` flag to hide/show fields

## Benefits

1. **Responsive**: Different layouts for different devices
2. **Flexible**: Fallback strategies ensure forms always render
3. **Extensible**: Custom component types and properties
4. **API-Friendly**: Internal forms don't need full layouts
5. **Developer-Friendly**: Auto-generation of default layouts
6. **Maintainable**: Clear separation between form definition and presentation
