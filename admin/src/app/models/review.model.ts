export interface Review {
  _id: string;
  id?: string;
  order_detail_id: {
    _id: string;
    product_name: string;
    order_id?: {
      _id: string;
      order_code: string;
      shipping_name: string;
      shipping_email: string;
    } | null;
  } | null;
  customer_id?: {
    _id: string;
    customer_code: string;
    full_name: string;
    phone: string;
  } | null;
  orderId?: string;
  orderCode?: string;
  productName?: string;
  customerId?: string;
  customerCode?: string;
  customerName?: string;
  customerPhone?: string;
  productImageUrl?: string;
  rating: number;
  content: string;
  status: 'pending' | 'approved' | 'rejected';
  images?: string[];
  videos?: string[];
  reply?: {
    content: string;
    repliedAt: Date;
  } | null;
  createdAt: Date;
}
