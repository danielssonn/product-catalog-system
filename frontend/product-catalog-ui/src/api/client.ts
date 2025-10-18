import axios, { type AxiosInstance } from 'axios';
import type { ProductTypeDefinition, ProductTypeCreate, Page } from '../types/productType';
import type { CatalogProduct, BulkCreateResponse } from '../types/catalog';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api/v1/admin';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor to include auth
    this.client.interceptors.request.use((config) => {
      const username = sessionStorage.getItem('username');
      const password = sessionStorage.getItem('password');

      if (username && password) {
        const token = btoa(`${username}:${password}`);
        config.headers.Authorization = `Basic ${token}`;
      }

      return config;
    });

    // Add response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Clear credentials and redirect to login
          sessionStorage.removeItem('username');
          sessionStorage.removeItem('password');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth methods
  async login(username: string, password: string): Promise<boolean> {
    try {
      // Test authentication by calling a simple endpoint
      const token = btoa(`${username}:${password}`);
      const response = await axios.get(`${API_BASE_URL}/product-types/active`, {
        headers: {
          Authorization: `Basic ${token}`,
        },
      });

      if (response.status === 200) {
        sessionStorage.setItem('username', username);
        sessionStorage.setItem('password', password);
        return true;
      }
      return false;
    } catch (error) {
      return false;
    }
  }

  logout() {
    sessionStorage.removeItem('username');
    sessionStorage.removeItem('password');
  }

  isAuthenticated(): boolean {
    return !!sessionStorage.getItem('username') && !!sessionStorage.getItem('password');
  }

  // Product Type API
  async getProductTypes(page: number = 0, size: number = 10): Promise<Page<ProductTypeDefinition>> {
    const response = await this.client.get(`/product-types?page=${page}&size=${size}`);
    return response.data;
  }

  async getActiveProductTypes(): Promise<ProductTypeDefinition[]> {
    const response = await this.client.get('/product-types/active');
    return response.data;
  }

  async getProductTypeByCode(typeCode: string): Promise<ProductTypeDefinition> {
    const response = await this.client.get(`/product-types/${typeCode}`);
    return response.data;
  }

  async createProductType(productType: ProductTypeCreate): Promise<ProductTypeDefinition> {
    const response = await this.client.post('/product-types', productType);
    return response.data;
  }

  async updateProductType(typeCode: string, productType: ProductTypeCreate): Promise<ProductTypeDefinition> {
    const response = await this.client.put(`/product-types/${typeCode}`, productType);
    return response.data;
  }

  async deactivateProductType(typeCode: string): Promise<void> {
    await this.client.patch(`/product-types/${typeCode}/deactivate`);
  }

  async reactivateProductType(typeCode: string): Promise<void> {
    await this.client.patch(`/product-types/${typeCode}/reactivate`);
  }

  async deleteProductType(typeCode: string): Promise<void> {
    await this.client.delete(`/product-types/${typeCode}`);
  }

  async getProductTypesByCategory(category: string): Promise<ProductTypeDefinition[]> {
    const response = await this.client.get(`/product-types/active/by-category/${category}`);
    return response.data;
  }

  // Catalog API
  async getCatalogProducts(page: number = 0, size: number = 10): Promise<Page<CatalogProduct>> {
    const response = await this.client.get(`/catalog?page=${page}&size=${size}`);
    return response.data;
  }

  async getAvailableCatalogProducts(): Promise<CatalogProduct[]> {
    const response = await this.client.get('/catalog/available');
    return response.data;
  }

  async getCatalogProductById(catalogProductId: string): Promise<CatalogProduct> {
    const response = await this.client.get(`/catalog/${catalogProductId}`);
    return response.data;
  }

  async createCatalogProduct(product: Partial<CatalogProduct>): Promise<CatalogProduct> {
    const response = await this.client.post('/catalog', product);
    return response.data;
  }

  async updateCatalogProduct(catalogProductId: string, product: Partial<CatalogProduct>): Promise<CatalogProduct> {
    const response = await this.client.put(`/catalog/${catalogProductId}`, product);
    return response.data;
  }

  async deleteCatalogProduct(catalogProductId: string): Promise<void> {
    await this.client.delete(`/catalog/${catalogProductId}`);
  }

  async bulkCreateCatalogProducts(products: Partial<CatalogProduct>[]): Promise<BulkCreateResponse> {
    const response = await this.client.post('/catalog/bulk', products);
    return response.data;
  }

  async getCatalogProductsByType(typeCode: string): Promise<Page<CatalogProduct>> {
    const response = await this.client.get(`/catalog/by-type/${typeCode}`);
    return response.data;
  }

  async getCatalogProductsByCategory(category: string): Promise<Page<CatalogProduct>> {
    const response = await this.client.get(`/catalog/by-category/${category}`);
    return response.data;
  }

  async getCatalogProductsByStatus(status: string): Promise<Page<CatalogProduct>> {
    const response = await this.client.get(`/catalog/by-status/${status}`);
    return response.data;
  }
}

export const apiClient = new ApiClient();
