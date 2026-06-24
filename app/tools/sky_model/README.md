# Sky-segmentation model

`app/src/main/assets/sky_mask_513.tflite` decides where the Garganta is allowed
to open: it only renders over real sky, never over ceilings, walls, or ground.

## Why this model

- Base: DeepLabV3 + MobileNetV2, trained on **ADE20K**
  (`deeplabv3_mnv2_ade20k_train_2018_12_03`, from `download.tensorflow.org`).
- ADE20K is the key: it has **separate `sky` and `ceiling` classes**, so an
  indoor white ceiling is not mistaken for sky — the exact failure we needed to
  avoid.
- Validated on real photos (sky / skyline / classroom-with-windows / indoor
  room / grass): sky is detected only where there is real sky; ceilings, walls,
  and grass stay at ~0.

## What the .tflite does

- Input: `ImageTensor`, uint8 `[1, 513, 513, 3]` (raw RGB; the graph handles its
  own normalisation). 513 px — at 257 px the mask was too noisy (tinted walls /
  grass); 513 px is clean.
- Output: `sky_prob`, float `[1, 129, 129, 1]` — a soft 0..1 sky probability.
  We take only the sky channel (softmax over the 151 ADE20K classes, then slice
  class 3) so the on-device tensor is tiny.

## Rebuilding

`build_sky_model.py` downloads the frozen ADE20K graph, appends
`softmax -> slice(sky) -> identity` and converts to TFLite. Run with
`tensorflow-cpu` + `pillow` installed.
