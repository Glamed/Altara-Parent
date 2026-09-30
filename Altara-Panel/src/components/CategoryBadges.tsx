import { Badge, Group } from '@mantine/core';
import { useCategories } from '../api/reports';
import type { ReportReason } from '../api/types';
import { titleCase } from '../format';

/** One badge per distinct category, with how many reasons cited it. */
export function CategoryBadges({ reasons }: { reasons: ReportReason[] }) {
  const { data: categories = [] } = useCategories();
  const names = new Map(categories.map((c) => [c.name, c.displayName]));

  const counts = new Map<string, number>();
  for (const reason of reasons) counts.set(reason.category, (counts.get(reason.category) ?? 0) + 1);

  return (
    <Group gap={4}>
      {[...counts].map(([category, count]) => (
        <Badge key={category} variant="default" size="sm">
          {names.get(category) ?? titleCase(category)}{count > 1 ? ` ×${count}` : ''}
        </Badge>
      ))}
    </Group>
  );
}
