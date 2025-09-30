# Product Catalog UI

Angular-based frontend application for Product Catalog Management.

## Setup

```bash
cd product-catalog-ui
npm install
ng serve
```

## Features

- Product catalog management
- Bundle configuration
- Cross-sell rule management
- Multi-tenant support
- Multi-channel awareness
- Audit trail viewer

## Structure

```
src/
├── app/
│   ├── core/                   # Core services and interceptors
│   ├── features/               # Feature modules
│   ├── shared/                 # Shared components and models
│   └── app.module.ts
├── assets/                     # Static assets
└── environments/               # Environment configurations
```

To generate the Angular project, run:
```bash
ng new product-catalog-ui --routing --style=scss
```