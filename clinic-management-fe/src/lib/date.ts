export function formatAppointmentDate(value?: string | null) {
  if (!value) return '—';

  const datePart = value.split('T')[0];
  const [year, month, day] = datePart.split('-');
  if (!year || !month || !day) {
    return value;
  }

  return `${day}/${month}/${year}`;
}