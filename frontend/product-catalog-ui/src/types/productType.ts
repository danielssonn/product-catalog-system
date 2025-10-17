export interface ProductTypeDefinition {
  id?: string;
  typeCode: string;
  name: string;
  description?: string;
  category: string;
  subcategory?: string;
  active: boolean;
  displayOrder?: number;
  icon?: string;
  tags?: string[];
  metadata?: Record<string, any>;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface ProductTypeCreate {
  typeCode: string;
  name: string;
  description?: string;
  category: string;
  subcategory?: string;
  active: boolean;
  displayOrder?: number;
  icon?: string;
  tags?: string[];
  metadata?: Record<string, any>;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
