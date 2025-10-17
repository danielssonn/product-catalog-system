export interface CatalogProduct {
  id?: string;
  catalogProductId: string;
  name: string;
  description?: string;
  category?: string;
  type: string;
  status: CatalogStatus;
  pricingTemplate?: PricingTemplate;
  availableFeatures?: Record<string, any>;
  defaultTerms?: Terms;
  configOptions?: ConfigOptions;
  supportedChannels?: string[];
  defaultEligibilityCriteria?: any;
  complianceTags?: string[];
  productTier?: string;
  requiresApproval?: boolean;
  documentationUrl?: string;
  relatedProducts?: string[];
  metadata?: Record<string, any>;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export const CatalogStatus = {
  PREVIEW: "PREVIEW",
  AVAILABLE: "AVAILABLE",
  DEPRECATED: "DEPRECATED",
  RETIRED: "RETIRED"
} as const;

export type CatalogStatus = typeof CatalogStatus[keyof typeof CatalogStatus];

export interface PricingTemplate {
  pricingType: string;
  currency: string;
  minInterestRate?: number;
  maxInterestRate?: number;
  defaultInterestRate?: number;
  minAnnualFee?: number;
  maxAnnualFee?: number;
  defaultAnnualFee?: number;
  minMonthlyFee?: number;
  maxMonthlyFee?: number;
  defaultMonthlyFee?: number;
}

export interface Terms {
  termsAndConditionsUrl?: string;
  disclosureUrl?: string;
  minTermLength?: number;
  maxTermLength?: number;
  defaultTermLength?: number;
  termUnit?: string;
  allowEarlyWithdrawal?: boolean;
  penaltyDescription?: string;
}

export interface ConfigOptions {
  canCustomizeName?: boolean;
  canCustomizeDescription?: boolean;
  canCustomizePricing?: boolean;
  canCustomizeFeatures?: boolean;
  canCustomizeTerms?: boolean;
}

export interface BulkCreateResponse {
  totalSubmitted: number;
  successCount: number;
  failureCount: number;
  errors: string[];
}
