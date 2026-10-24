const CACHE_NAME = 'photomagic-pwa-v8';
const ASSETS = [
  './manifest.json',
  './icon-192.png',
  './icon-512.png'
];

self.addEventListener('install', (e) => {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS))
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) return caches.delete(key);
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Always Network-First (no cache) for index.html / HTML navigation so updates are 100% instant
self.addEventListener('fetch', (e) => {
  if (e.request.mode === 'navigate' || e.request.destination === 'document' || e.request.url.includes('index.html')) {
    e.respondWith(
      fetch(e.request).catch(() => caches.match('./index.html'))
    );
    return;
  }

  e.respondWith(
    caches.match(e.request).then((cachedRes) => {
      return cachedRes || fetch(e.request);
    })
  );
});
