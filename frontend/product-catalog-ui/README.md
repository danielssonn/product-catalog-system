# Product Catalog Admin UI

A modern React-based admin interface for managing product types and catalog products in the Product Catalog System.

## Features

- **Product Type Management**: Create, edit, activate/deactivate, and delete product types
- **Catalog Product Management**: Full CRUD operations for catalog products
- **Authentication**: Secure login with HTTP Basic Authentication
- **Responsive Design**: Mobile-friendly interface built with Tailwind CSS
- **Real-time Updates**: Dynamic data loading and error handling

## Tech Stack

- **React 18** - Modern UI library with hooks
- **TypeScript** - Type-safe development
- **Vite** - Fast build tool and dev server
- **React Router** - Client-side routing
- **Tailwind CSS v4** - Utility-first CSS framework
- **Axios** - HTTP client for API requests

## Quick Start

### 1. Install Dependencies

```bash
npm install
```

### 2. Start Development Server

```bash
npm run dev
```

The application will be available at [http://localhost:5173](http://localhost:5173)

### 3. Login

Default credentials:
- **Username**: `admin`
- **Password**: `admin123`

### 4. Build for Production

```bash
npm run build
```

## Prerequisites

Ensure the Product Catalog Backend is running:

```bash
cd backend
mvn clean install
docker-compose up -d
```

The backend API should be accessible at `http://localhost:8082/api/v1/admin`

## Available Routes

| Route | Description |
|-------|-------------|
| `/login` | Authentication page |
| `/` | Dashboard with statistics |
| `/product-types` | List all product types |
| `/product-types/new` | Create new product type |
| `/product-types/:typeCode` | Edit existing product type |
| `/catalog` | List all catalog products |
| `/catalog/new` | Create new catalog product |
| `/catalog/:catalogProductId` | Edit existing catalog product |

## Application Structure

```
src/
├── api/
│   └── client.ts              # Complete API client (31 methods)
├── components/
│   ├── Login.tsx              # Authentication
│   ├── Layout.tsx             # Main layout
│   ├── Dashboard.tsx          # Statistics dashboard
│   ├── ProductTypeList.tsx    # Product type management
│   ├── ProductTypeForm.tsx    # Product type form
│   ├── CatalogList.tsx        # Catalog management
│   └── CatalogForm.tsx        # Catalog product form
├── types/
│   ├── productType.ts         # Product type types
│   └── catalog.ts             # Catalog types
├── App.tsx                    # Routing configuration
└── index.css                  # Global styles
```

## Environment Variables

Create a `.env` file to override defaults:

```bash
VITE_API_BASE_URL=http://localhost:8082/api/v1/admin
```

## API Client

The application includes a complete API client with 31 methods:

### Product Type API (11 methods)
- CRUD operations for product types
- Activate/deactivate functionality
- Filter by category

### Catalog API (10 methods)
- CRUD operations for catalog products
- Bulk create support
- Filter by type, category, and status

### Authentication (3 methods)
- Login, logout, and auth check

## Development

### Hot Module Replacement
Vite provides instant HMR during development. Changes are reflected immediately without full page reload.

### TypeScript
All components are strongly typed. The API client includes full type definitions matching the backend models.

### Styling
Tailwind CSS v4 with the new `@import "tailwindcss"` syntax. Custom theme configured in `tailwind.config.js`.

## Troubleshooting

### API Connection Issues
Ensure:
1. Backend is running on `http://localhost:8082`
2. You're logged in with valid credentials
3. CORS is configured on the backend

### Port Already in Use
```bash
npm run dev -- --port 3000
```

### Build Issues
```bash
rm -rf node_modules package-lock.json
npm install
```
