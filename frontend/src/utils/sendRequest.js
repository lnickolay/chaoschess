export const sendRequest = async (endpoint, method = "GET", body = null) => {
  const response = await fetch(`http://localhost:8080/api/game/${endpoint}`, {
    method: method,
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : null,
  });

  // Check for HTTP errors (4xx, 5xx)
  if (!response.ok) {
    // Throws an exception that will be caught in the caller's catch block and
    // attempts to parse the JSON error body (if present)
    const errorBody = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(`HTTP Error ${response.status}: ${errorBody.message || "Unknown error"}`);
  }

  // If 204 (no content) is returned, do not parse anything
  if (response.status === 204) {
    return null;
  }

  // If it is a successful request with a body, parse JSON.
  return response.json();
};
