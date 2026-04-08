import { HttpErrorResponse } from '@angular/common/http';

/**
 * Normalise les messages d’erreur HTTP Angular / Spring Boot (X-Error-Message, corps JSON, etc.).
 */
export function getHttpErrorMessage(
  error: unknown,
  defaultMessage = 'Une erreur est survenue'
): string {
  if (error == null) {
    return defaultMessage;
  }

  if (error instanceof HttpErrorResponse) {
    const headerMsg = error.headers?.get('X-Error-Message');
    if (headerMsg) {
      return headerMsg;
    }
    return parseSpringStyleBody(error.error, defaultMessage, error.message, error.statusText);
  }

  return defaultMessage;
}

function parseSpringStyleBody(
  body: unknown,
  defaultMessage: string,
  rawMessage?: unknown,
  statusText?: unknown
): string {
  if (body != null && typeof body === 'object') {
    const o = body as Record<string, unknown>;
    const msg = o['message'];
    if (typeof msg === 'string') {
      return msg;
    }
    if (Array.isArray(msg)) {
      return msg.map(String).join(', ');
    }
    const nested = o['error'];
    if (typeof nested === 'string') {
      if (
        nested !== 'Bad Request' &&
        nested !== 'Internal Server Error' &&
        nested !== 'Not Found' &&
        nested !== 'Forbidden'
      ) {
        return nested;
      }
    }
    const detail = o['detail'];
    if (typeof detail === 'string') {
      return detail;
    }
  } else if (typeof body === 'string') {
    return body;
  }

  if (typeof rawMessage === 'string') {
    if (
      !rawMessage.includes('Http failure') &&
      !rawMessage.includes('HttpErrorResponse') &&
      !rawMessage.includes('status code')
    ) {
      return rawMessage;
    }
  }

  if (typeof statusText === 'string') {
    if (
      statusText !== 'Bad Request' &&
      statusText !== 'Internal Server Error' &&
      statusText !== 'OK'
    ) {
      return statusText;
    }
  }

  return defaultMessage;
}
