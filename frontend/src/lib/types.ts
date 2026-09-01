/**
 * The shapes the Java API returns.
 *
 * These mirror the records in `ng.edu.unilag.tradeup.web.dto`. Keeping them in
 * one file means a change on the backend surfaces here as a type error rather
 * than as an undefined at runtime.
 */

export interface UserSummary {
  id: number;
  fullName: string;
  initials: string;
  department: string | null;
  campusLocation: string | null;
  completedTrades: number;
  admin: boolean;
}

export interface UserProfile {
  id: number;
  fullName: string;
  initials: string;
  department: string | null;
  campusLocation: string | null;
  bio: string | null;
  completedTrades: number;
  joinedAt: string;
  activeListings: ListingCard[];
}

export interface AuthResponse {
  token: string;
  expiresInSeconds: number;
  user: UserSummary;
}

export interface ListingCard {
  id: number;
  reference: string;
  title: string;
  categoryName: string;
  categoryLabel: string;
  categorySlug: string;
  conditionName: string;
  conditionLabel: string;
  intentName: TradeIntent;
  intentLabel: string;
  statusName: ListingStatus;
  statusLabel: string;
  priceKobo: number | null;
  swapWanted: string | null;
  pickupLocation: string | null;
  primaryImageUrl: string | null;
  ownerGeneration: number;
  viewCount: number;
  co2SavedKg: number;
  owner: UserSummary;
  savedByViewer: boolean;
  createdAt: string;
}

export interface ListingDetail extends Omit<ListingCard, "primaryImageUrl" | "savedByViewer"> {
  description: string;
  conditionDescription: string;
  acceptsCash: boolean;
  acceptsSwap: boolean;
  imageUrls: string[];
  ownedByViewer: boolean;
  savedByViewer: boolean;
  savedCount: number;
  pendingOfferCount: number;
  updatedAt: string;
}

export type TradeIntent = "SELL" | "SWAP" | "BOTH";

export type ListingStatus = "DRAFT" | "ACTIVE" | "RESERVED" | "COMPLETED" | "REMOVED" | "FLAGGED";

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface CategoryOption {
  name: string;
  label: string;
  slug: string;
  availableCount: number;
}

export interface Option {
  name: string;
  label: string;
  description: string | null;
}

export interface ReferenceData {
  categories: CategoryOption[];
  conditions: Option[];
  intents: Option[];
}

export interface CategoryImpact {
  name: string;
  label: string;
  slug: string;
  itemsRehomed: number;
  co2SavedKg: number;
}

export interface ImpactSnapshot {
  itemsRehomed: number;
  itemsAvailableNow: number;
  studentsRegistered: number;
  co2SavedKg: number;
  wasteDivertedKg: number;
  moneyKeptInPocketsKobo: number;
  byCategory: CategoryImpact[];
}

export interface TeamMember {
  matricNumber: string;
  fullName: string;
  department: string;
  role: string;
}

export interface OfferView {
  id: number;
  kind: "CASH" | "SWAP";
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "WITHDRAWN";
  amountKobo: number | null;
  note: string | null;
  offeredBy: UserSummary;
  listingId: number;
  listingTitle: string;
  listingReference: string;
  listingImageUrl: string | null;
  offeredListingId: number | null;
  offeredListingTitle: string | null;
  offeredListingImageUrl: string | null;
  createdAt: string;
}

export interface MessageView {
  id: number;
  body: string;
  sender: UserSummary;
  mine: boolean;
  read: boolean;
  sentAt: string;
}

export interface ConversationSummary {
  id: number;
  listingId: number;
  listingTitle: string;
  listingReference: string;
  listingImageUrl: string | null;
  counterpart: UserSummary;
  lastMessagePreview: string | null;
  lastMessageAt: string;
  unreadCount: number;
}

export interface ConversationDetail {
  id: number;
  listing: ListingCard;
  counterpart: UserSummary;
  messages: MessageView[];
}

export interface DashboardSummary {
  activeListings: number;
  reservedListings: number;
  completedTrades: number;
  unreadMessages: number;
  pendingOffersReceived: number;
  personalCo2SavedKg: number;
  recentListings: ListingCard[];
  offersAwaitingYou: OfferView[];
}

export interface ReportView {
  id: number;
  reason: string;
  reasonLabel: string;
  details: string | null;
  status: "OPEN" | "UPHELD" | "DISMISSED";
  reporter: UserSummary;
  listingId: number;
  listingTitle: string;
  listingReference: string;
  listingImageUrl: string | null;
  listingStatus: ListingStatus;
  listingOwner: UserSummary;
  moderatorNote: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

/** The error envelope every failed request returns. */
export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
  timestamp: string;
}

export interface ListingInput {
  title: string;
  description: string;
  category: string;
  itemCondition: string;
  intent: TradeIntent;
  priceKobo: number | null;
  swapWanted: string | null;
  pickupLocation: string | null;
  imageUrls: string[];
}
