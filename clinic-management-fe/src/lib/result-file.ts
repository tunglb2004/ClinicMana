type ResultFileCarrier = {
  resultFileUrls?: string[];
  resultFileUrl?: string | null;
};

function expandRawFileValue(rawValue?: string | null): string[] {
  if (!rawValue) {
    return [];
  }

  const trimmed = rawValue.trim();
  if (!trimmed) {
    return [];
  }

  if (trimmed.startsWith('[') || (trimmed.startsWith('"') && trimmed.endsWith('"'))) {
    try {
      const parsed = JSON.parse(trimmed) as unknown;
      if (Array.isArray(parsed)) {
        return parsed.flatMap((value) => expandRawFileValue(typeof value === 'string' ? value : null));
      }
      if (typeof parsed === 'string') {
        return expandRawFileValue(parsed);
      }
    } catch {
      // Fall back to the raw string below.
    }
  }

  if (trimmed.includes('\n')) {
    return trimmed.split(/\r?\n/).flatMap((value) => expandRawFileValue(value));
  }

  return [trimmed];
}

function extractResultFileName(fileUrl: string): string | null {
  try {
    const url = new URL(fileUrl);
    const candidate = url.pathname.split('/').pop();
    if (candidate) {
      return candidate;
    }
  } catch {
    // Ignore invalid URL parsing and fall back to string splitting below.
  }

  const sanitizedUrl = fileUrl.split('#')[0]?.split('?')[0] ?? '';
  return sanitizedUrl.split('/').pop() ?? null;
}

export function safeDecodeURIComponent(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

export function getNormalizedResultFileUrls(source?: ResultFileCarrier | null): string[] {
  if (!source) {
    return [];
  }

  const rawValues = [...(source.resultFileUrls ?? []), source.resultFileUrl ?? null];
  const seen = new Set<string>();
  const normalized: string[] = [];

  for (const rawValue of rawValues) {
    for (const fileUrl of expandRawFileValue(rawValue)) {
      if (!fileUrl || seen.has(fileUrl)) {
        continue;
      }
      seen.add(fileUrl);
      normalized.push(fileUrl);
    }
  }

  return normalized;
}

export function normalizeResultFileCarrier<T extends ResultFileCarrier>(source: T): T {
  const resultFileUrls = getNormalizedResultFileUrls(source);
  return {
    ...source,
    resultFileUrls,
    resultFileUrl: resultFileUrls[resultFileUrls.length - 1] ?? null,
  };
}

export function getResultFileName(fileUrl: string, fallbackBase: string, index: number): string {
  const rawName = extractResultFileName(fileUrl);
  if (!rawName) {
    return `${fallbackBase} - file ${index + 1}`;
  }

  return safeDecodeURIComponent(rawName);
}

function getDefaultResultFileOrigin(): string {
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL;
  if (apiBaseUrl) {
    try {
      const url = new URL(apiBaseUrl);
      url.hash = '';
      url.search = '';
      url.pathname = url.pathname.replace(/\/api\/?$/i, '').replace(/\/$/, '');
      return url.toString().replace(/\/$/, '');
    } catch {
      // Fall back to the browser origin below.
    }
  }

  return typeof window !== 'undefined' ? window.location.origin : '';
}

export function resolveResultFileUrl(fileUrl: string, origin?: string): string {
  const trimmed = fileUrl.trim();
  if (!trimmed) {
    return '';
  }

  if (/^(https?:|blob:|data:)/i.test(trimmed)) {
    return encodeURI(trimmed);
  }

  if (trimmed.startsWith('//')) {
    const protocol = typeof window !== 'undefined' ? window.location.protocol : 'https:';
    return encodeURI(`${protocol}${trimmed}`);
  }

  const baseOrigin = origin ?? getDefaultResultFileOrigin();
  if (!baseOrigin) {
    return encodeURI(trimmed);
  }

  if (trimmed.startsWith('/')) {
    return encodeURI(`${baseOrigin}${trimmed}`);
  }

  return encodeURI(`${baseOrigin}/${trimmed}`);
}

export function triggerResultFileDownload(fileUrl: string, fallbackBase: string) {
  const link = document.createElement('a');
  link.href = resolveResultFileUrl(fileUrl);
  link.download = getResultFileName(fileUrl, fallbackBase, 0);
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
