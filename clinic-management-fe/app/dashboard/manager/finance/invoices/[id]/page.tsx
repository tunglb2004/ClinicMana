'use client';

import { use, Suspense } from 'react';
import { InvoiceDetailContent } from '../../../../cashier/invoices/[id]/page';

export default function ManagerInvoiceDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);

  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center bg-gray-50 p-8">
        <div className="rounded border-2 border-black bg-white p-8 font-bold uppercase shadow-[4px_4px_0_0_rgba(0,0,0,1)]">
          Đang tải...
        </div>
      </div>
    }>
      <InvoiceDetailContent id={id} forceHistory />
    </Suspense>
  );
}
