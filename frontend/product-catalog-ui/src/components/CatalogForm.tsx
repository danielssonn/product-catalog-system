import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { CatalogProduct } from '../types/catalog';
import { CatalogStatus } from '../types/catalog';
import type { ProductTypeDefinition } from '../types/productType';

export function CatalogForm() {
  const { catalogProductId } = useParams<{ catalogProductId: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [productTypes, setProductTypes] = useState<ProductTypeDefinition[]>([]);

  const [formData, setFormData] = useState<Partial<CatalogProduct>>({
    catalogProductId: '',
    name: '',
    description: '',
    category: '',
    type: '',
    status: CatalogStatus.PREVIEW,
    pricingTemplate: {
      pricingType: 'FIXED',
      currency: 'USD',
      defaultInterestRate: 0,
    },
    supportedChannels: [],
  });

  useEffect(() => {
    loadProductTypes();
    if (catalogProductId && catalogProductId !== 'new') {
      loadCatalogProduct();
    }
  }, [catalogProductId]);

  const loadProductTypes = async () => {
    try {
      const types = await apiClient.getActiveProductTypes();
      setProductTypes(types);
    } catch (error) {
      console.error('Failed to load product types', error);
    }
  };

  const loadCatalogProduct = async () => {
    if (!catalogProductId || catalogProductId === 'new') return;

    try {
      const data = await apiClient.getCatalogProductById(catalogProductId);
      setFormData(data);
    } catch (error) {
      setError('Failed to load catalog product');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (catalogProductId && catalogProductId !== 'new') {
        await apiClient.updateCatalogProduct(catalogProductId, formData);
      } else {
        await apiClient.createCatalogProduct(formData);
      }
      navigate('/catalog');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save catalog product');
    } finally {
      setLoading(false);
    }
  };

  const isEditing = catalogProductId && catalogProductId !== 'new';

  return (
    <div className="max-w-4xl">
      <div className="mb-6">
        <h2 className="text-3xl font-bold text-gray-900">
          {isEditing ? 'Edit Catalog Product' : 'Create Catalog Product'}
        </h2>
        <p className="text-gray-600 mt-1">
          {isEditing ? `Editing ${catalogProductId}` : 'Add a new product to the catalog'}
        </p>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-lg">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 space-y-6">
        {/* Basic Information */}
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-gray-900 border-b pb-2">Basic Information</h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Catalog Product ID *
              </label>
              <input
                type="text"
                value={formData.catalogProductId}
                onChange={(e) => setFormData({...formData, catalogProductId: e.target.value})}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono"
                placeholder="cat-checking-001"
                required
                disabled={!!isEditing}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Name *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({...formData, name: e.target.value})}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Premium Checking Account"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({...formData, description: e.target.value})}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              rows={3}
              placeholder="Description of the product..."
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Product Type *
              </label>
              <select
                value={formData.type}
                onChange={(e) => setFormData({...formData, type: e.target.value})}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="">Select type...</option>
                {productTypes.map((type) => (
                  <option key={type.typeCode} value={type.typeCode}>
                    {type.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Category
              </label>
              <input
                type="text"
                value={formData.category}
                onChange={(e) => setFormData({...formData, category: e.target.value})}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="DEPOSIT, LOAN, etc."
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Status *
              </label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({...formData, status: e.target.value as CatalogStatus})}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value={CatalogStatus.PREVIEW}>PREVIEW</option>
                <option value={CatalogStatus.AVAILABLE}>AVAILABLE</option>
                <option value={CatalogStatus.DEPRECATED}>DEPRECATED</option>
                <option value={CatalogStatus.RETIRED}>RETIRED</option>
              </select>
            </div>
          </div>
        </div>

        {/* Pricing Information */}
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-gray-900 border-b pb-2">Pricing</h3>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Default Interest Rate (%)
              </label>
              <input
                type="number"
                step="0.01"
                value={formData.pricingTemplate?.defaultInterestRate || 0}
                onChange={(e) => setFormData({
                  ...formData,
                  pricingTemplate: {
                    ...formData.pricingTemplate!,
                    pricingType: formData.pricingTemplate?.pricingType || 'FIXED',
                    currency: formData.pricingTemplate?.currency || 'USD',
                    defaultInterestRate: parseFloat(e.target.value)
                  }
                })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Currency
              </label>
              <input
                type="text"
                value={formData.pricingTemplate?.currency || 'USD'}
                onChange={(e) => setFormData({
                  ...formData,
                  pricingTemplate: {
                    ...formData.pricingTemplate!,
                    pricingType: formData.pricingTemplate?.pricingType || 'FIXED',
                    currency: e.target.value,
                    defaultInterestRate: formData.pricingTemplate?.defaultInterestRate
                  }
                })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Pricing Type
              </label>
              <select
                value={formData.pricingTemplate?.pricingType || 'FIXED'}
                onChange={(e) => setFormData({
                  ...formData,
                  pricingTemplate: {
                    ...formData.pricingTemplate!,
                    pricingType: e.target.value,
                    currency: formData.pricingTemplate?.currency || 'USD',
                    defaultInterestRate: formData.pricingTemplate?.defaultInterestRate
                  }
                })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="FIXED">Fixed</option>
                <option value="VARIABLE">Variable</option>
                <option value="TIERED">Tiered</option>
              </select>
            </div>
          </div>
        </div>

        {/* Available Channels */}
        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-gray-900 border-b pb-2">Channels</h3>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Supported Channels (comma-separated)
            </label>
            <input
              type="text"
              value={formData.supportedChannels?.join(', ')}
              onChange={(e) => setFormData({
                ...formData,
                supportedChannels: e.target.value.split(',').map(c => c.trim()).filter(Boolean)
              })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="WEB, MOBILE, BRANCH, ATM"
            />
          </div>
        </div>

        {/* Form Actions */}
        <div className="flex items-center justify-end space-x-4 pt-4 border-t border-gray-200">
          <button
            type="button"
            onClick={() => navigate('/catalog')}
            className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Saving...' : (isEditing ? 'Update' : 'Create')}
          </button>
        </div>
      </form>
    </div>
  );
}
