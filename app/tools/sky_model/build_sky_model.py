import os, numpy as np, tensorflow as tf
from PIL import Image
BASE=os.path.dirname(os.path.abspath(__file__)); SIZE,SKY=513,3
PB=os.path.join(BASE,"deeplabv3_mnv2_ade20k_train_2018_12_03","frozen_inference_graph.pb")
OUT=os.path.join(BASE,"sky_mask_513.tflite")

gd=tf.compat.v1.GraphDef()
gd.ParseFromString(open(PB,"rb").read())
g=tf.Graph()
with g.as_default():
    tf.import_graph_def(gd,name="")
    logits=g.get_tensor_by_name("logits/semantic/BiasAdd:0")     # [1,h,w,151]
    sky=tf.nn.softmax(logits)[...,SKY:SKY+1]                       # [1,h,w,1]
    sky=tf.identity(sky,name="sky_prob")
gd2=g.as_graph_def()
tmp=os.path.join(BASE,"_sky_graph.pb"); open(tmp,"wb").write(gd2.SerializeToString())

c=tf.compat.v1.lite.TFLiteConverter.from_frozen_graph(tmp,["ImageTensor"],["sky_prob"],{"ImageTensor":[1,SIZE,SIZE,3]})
c.experimental_new_converter=True
data=c.convert(); open(OUT,"wb").write(data)
print(f"OK {OUT}  {len(data)/1e6:.1f}MB")

it=tf.lite.Interpreter(model_path=OUT); it.allocate_tensors()
ind=it.get_input_details()[0]; outd=it.get_output_details()[0]
print("input",ind["shape"],ind["dtype"],"-> output",outd["shape"],outd["dtype"])
# sanity: run on street.png (has sky) and roomD (indoor)
for n in ["street.png","roomD.jpg","indoorA.jpg","nature.jpg"]:
    im=Image.open(os.path.join(BASE,"test",n)).convert("RGB").resize((SIZE,SIZE),Image.BILINEAR)
    it.set_tensor(ind["index"],np.asarray(im,np.uint8)[None]); it.invoke()
    p=it.get_tensor(outd["index"])[0,...,0]
    print(f"  {n:14s} sky>0.5 area={ (p>0.5).mean()*100:4.1f}%  max={p.max():.2f}")
