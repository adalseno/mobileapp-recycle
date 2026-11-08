# Suggestions for the BCRS Malta website

These are ideas for making [bcrsmalta.mt](https://bcrsmalta.mt/) easier to use on a phone,
where most people check a container or look for a return point. They're written for BCRS
Malta and their web team, and each one is independent.

The current site already has the two features people need on the go: the
[barcode search](https://bcrsmalta.mt/#barcode-deposit) and the
[return-point map](https://bcrsmalta.mt/locate/). The suggestions make those faster to use
from a phone, with no native app required.

## 1. Scan the barcode with the camera

Add a **Scan** button next to the barcode search box, so people don't have to type 13 digits
while holding a bottle.

- Android (Chrome): the browser's built-in
  [`BarcodeDetector`](https://developer.mozilla.org/docs/Web/API/BarcodeDetector) API reads
  EAN-13, EAN-8, UPC-A and UPC-E codes from the camera.
- iPhone (Safari) has no `BarcodeDetector`, so a small JavaScript or WebAssembly library
  (for example ZXing) is needed as a fallback.
- After a successful scan, run the existing search automatically.

## 2. "Near me" on the return-point map

On `/locate/`:

- A **Near me** button that uses the phone's location (with permission) and lists the
  nearest return points by distance.
- **Open now / closed** next to each point, based on the opening hours already shown.
- A **Directions** link on each point that opens Google Maps or Apple Maps.

## 3. Installable web app (PWA)

Add a web app manifest, icons and a service worker so the site can be added to the home
screen and opens like an app. The return-point list can be cached so it still shows with a
weak signal inside a shop.

## 4. QR codes on machines and collection points

A sticker with a QR code linking straight to the barcode check (and, ideally, to that return
point on the map) puts the website one tap away when it's needed.

## 5. Allow pinch-to-zoom

The site currently sets `maximum-scale=1.0, user-scalable=no` in its viewport tag, which
stops people from zooming. That's an accessibility problem (WCAG 1.4.4, Resize text) for
people with poor eyesight. Removing those two values fixes it.

## 6. Richer barcode results

Today the search answers only "eligible" or "not eligible". It would help to also show the
product name, container type (plastic, can, glass) and the deposit amount.

## 7. Open data (optional)

Publishing the return-point list (locations, type, opening hours) and the barcode check as
simple public data, for example JSON, would let others build on them: maps, accessibility
tools, or apps like this one. Malta's open data portal could host it.

---

## About this app

[Bottle Checker](../README.md) is a free, open-source Android and iOS app that does the
barcode part with the phone's native scanner, and links to the official return-point map.
It uses the same barcode search as the website, sends one request per check, and states that
it isn't affiliated with BCRS Malta.

It has been tested on Android 17. The iOS version builds, but hasn't been tested on an iPhone
or iPad yet.

The code is under the [Apache 2.0 licence](../LICENSE), so BCRS is welcome to adopt or
rebrand it at no cost. If BCRS would rather the app didn't use the website's search, or
offers an official way to do it, the app will be changed accordingly.
