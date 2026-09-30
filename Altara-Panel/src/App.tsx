import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { HistoryPage } from './pages/HistoryPage';
import { QueuePage } from './pages/QueuePage';
import { ReportPage } from './pages/ReportPage';
import { SignInPage } from './pages/SignInPage';
import { useStaff } from './staff/StaffContext';

export function App() {
  const { staff } = useStaff();
  if (!staff) return <SignInPage />;

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<QueuePage />} />
        <Route path="reports/:id" element={<ReportPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
