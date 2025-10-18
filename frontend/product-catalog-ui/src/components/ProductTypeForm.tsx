import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { ProductTypeCreate } from '../types/productType';

export function ProductTypeForm() {
  const { typeCode } = useParams<{ typeCode: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [formData, setFormData] = useState<ProductTypeCreate>({
    typeCode: '',
    name: '',
    description: '',
    category: 'ACCOUNT',
    subcategory: '',
    active: true,
    displayOrder: 1,
    icon: '',
    tags: [],
  });

  useEffect(() => {
    if (typeCode && typeCode !== 'new') {
      loadProductType();
    }
  }, [typeCode]);

  const loadProductType = async () => {
    if (!typeCode || typeCode === 'new') return;

    try {
      const data = await apiClient.getProductTypeByCode(typeCode);
      setFormData({
        typeCode: data.typeCode,
        name: data.name,
        description: data.description || '',
        category: data.category,
        subcategory: data.subcategory || '',
        active: data.active,
        displayOrder: data.displayOrder || 1,
        icon: data.icon || '',
        tags: data.tags || [],
      });
    } catch (error) {
      setError('Failed to load product type');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (typeCode && typeCode !== 'new') {
        await apiClient.updateProductType(typeCode, formData);
      } else {
        await apiClient.createProductType(formData);
      }
      navigate('/product-types');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save product type');
    } finally {
      setLoading(false);
    }
  };

  const isEditing = typeCode && typeCode !== 'new';

  return (
    <div className="max-w-3xl">
      <div className="mb-6">
        <h2 className="text-3xl font-bold text-gray-900">
          {isEditing ? 'Edit Product Type' : 'Create Product Type'}
        </h2>
        <p className="text-gray-600 mt-1">
          {isEditing ? `Editing ${typeCode}` : 'Add a new product type to the system'}
        </p>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-lg">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Type Code *
            </label>
            <input
              type="text"
              value={formData.typeCode}
              onChange={(e) => setFormData({...formData, typeCode: e.target.value.toUpperCase()})}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono"
              placeholder="CHECKING_ACCOUNT"
              required
              disabled={!!isEditing}
            />
            <p className="text-xs text-gray-500 mt-1">Uppercase letters, numbers, and underscores only</p>
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
              placeholder="Checking Account"
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
            placeholder="Description of this product type..."
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Category *
            </label>
            <select
              value={formData.category}
              onChange={(e) => setFormData({...formData, category: e.target.value})}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            >
              <option value="ACCOUNT">ACCOUNT</option>
              <option value="LOAN">LOAN</option>
              <option value="LENDING">LENDING</option>
              <option value="PAYMENT">PAYMENT</option>
              <option value="CARD">CARD</option>
              <option value="SERVICE">SERVICE</option>
              <option value="OTHER">OTHER</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Subcategory
            </label>
            <input
              type="text"
              value={formData.subcategory}
              onChange={(e) => setFormData({...formData, subcategory: e.target.value})}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="DEPOSIT, CONSUMER, etc."
            />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Icon
            </label>
            <input
              type="text"
              value={formData.icon}
              onChange={(e) => setFormData({...formData, icon: e.target.value})}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="account_balance"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Display Order
            </label>
            <input
              type="number"
              value={formData.displayOrder}
              onChange={(e) => setFormData({...formData, displayOrder: parseInt(e.target.value)})}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              min="1"
            />
          </div>

          <div className="flex items-center">
            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="checkbox"
                checked={formData.active}
                onChange={(e) => setFormData({...formData, active: e.target.checked})}
                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700">Active</span>
            </label>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Tags (comma-separated)
          </label>
          <input
            type="text"
            value={formData.tags?.join(', ')}
            onChange={(e) => setFormData({...formData, tags: e.target.value.split(',').map(t => t.trim()).filter(Boolean)})}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="account, deposit, checking"
          />
        </div>

        <div className="flex items-center justify-end space-x-4 pt-4 border-t border-gray-200">
          <button
            type="button"
            onClick={() => navigate('/product-types')}
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
