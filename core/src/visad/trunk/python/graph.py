from visad import RealType, RealTupleType, FunctionType, FieldImpl, ScalarMap, Display
from visad.util import AnimationWidget
from visad.python.JPythonMethods import *
from javax.swing import JFrame, JPanel
from java.awt import BorderLayout, FlowLayout
import subs

def image(img, panel=None, colortable=None):
  return

def scatter(data_1, data_2, panel=None, size=None, xlabel=None, ylabel=None, title="VisAD Scatter"):

  rng_1 = data_1.getType().getRange().toString()
  rng_2 = data_2.getType().getRange().toString()
  maps = subs.makeMaps(getRealType(rng_1),"x", getRealType(rng_2),"y")
  disp = subs.makeDisplay(maps)
  subs.addData("first", data_1, disp)
  subs.addData("second", data_2, disp)

  subs.showDisplay(disp,400,500,title)

  return

# quick look histogram - only first range component is used.

def histogram(data, bins=20, title="VisAD Histogram", color=None, panel=None):

  from java.lang.Math import abs

  x=[]
  y=[]

  h = hist(data, [0], [bins])
  dom = getDomain(h)
  d = dom.getSamples()
  step2 = dom.getStep()/2

  hmin = h[0].getValue()
  hmax = hmin

  for i in range(0,len(h)):
   xm = d[0][i]-step2
   xp = d[0][i]+step2
   x.append(xm)
   y.append(0)
   x.append(xm)
   hval = h[i].getValue()
   if hval < hmin: hmin = hval
   if hval > hmax: hmax = hval
   y.append(hval)
   x.append(xp)
   y.append(hval)
   x.append(xp)
   y.append(0)

  domt = domainType(h)
  rngt = rangeType(h)

  xaxis = ScalarMap(domt[0], Display.XAxis)
  yaxis = ScalarMap(rngt, Display.YAxis)

  yaxis.setRange(hmin, hmax + abs(hmax * .05))

  disp = subs.makeDisplay( (xaxis, yaxis) )
  subs.drawLine(disp, (x,y), mathtype=(domt[0],rngt), color=color)
  showAxesScales(disp,1)
  subs.maximizeBox(disp,.65)
  subs.showDisplay(disp, title=title )

  return

def piechart(data, panel=None):
  return

def lineplot(sdata, panel=None, colortable=None):
  return

def contour(data, panel=None, interval=None, size=None):
  return

# animation(data) creates a VisAD animation of the items in the data list/tuple
# if panel is not None, then it will return a JPanel with the images
# and AnimationWidget in it
def animation(data, panel=None, title="VisAD Animation"):


  num_frames = len(data)

  frames = RealType.getRealType("frames")
  frames_type = RealTupleType( frames )

  image_type = data[0].getType()
  ndom = domainDimension(data[0])

  if ndom != 2:
    print "domain dimension must be 2!"
    return None

  dom_1 = RealType.getRealType(domainType(data[0],0) )
  dom_2 = RealType.getRealType(domainType(data[0],1)) 

  nrng = rangeDimension(data[0])
  if (nrng != 3) and (nrng != 1):
    print "range dimension must be 1 or 3"
    return None

  # now create display scalar maps
  maps = None
  rng_1 = rangeType(data[0],0)
  if nrng == 3:
    rng_2 = rangeType(data[0],1)
    rng_3 = rangeType(data[0],2)
    rng_red = None
    if (rng_1 == "Red"): rng_red = rng_1
    if (rng_2 == "Red"): rng_red = rng_2
    if (rng_3 == "Red"): rng_red = rng_3
    rng_green = None
    if (rng_1 == "Green"): rng_green = rng_1
    if (rng_2 == "Green"): rng_green = rng_2
    if (rng_3 == "Green"): rng_green = rng_3
    rng_blue = None
    if (rng_1 == "Blue"): rng_blue = rng_1
    if (rng_2 == "Blue"): rng_blue = rng_2
    if (rng_3 == "Blue"): rng_blue = rng_3

    if (rng_red is None) or (rng_green is None) or (rng_blue is None):
      print "3 Range components must be Red, Green and Blue"

    else:
      maps = subs.makeMaps(dom_1,"x", dom_2,"y", RealType.getRealType(rng_red), "red", RealType.getRealType(rng_green), "green", RealType.getRealType(rng_blue), "blue")

  else:
    maps = subs.makeMaps(dom_1,"x", dom_2, "y", RealType.getRealType(rng_1), "rgb")

  frame_images = FunctionType(frames_type, image_type)
  frame_set = makeDomain(frames, 0, num_frames-1, num_frames)
  frame_seq = FieldImpl(frame_images, frame_set)

  for i in range(0,num_frames):
    frame_seq.setSample(i, data[i])

  disp = subs.makeDisplay(maps)
  animap = ScalarMap(frames, Display.Animation)
  disp.addMap(animap)

  refimg = subs.addData("VisAD_Animation", frame_seq, disp)
  widget = AnimationWidget(animap, 500) 

  panel = JPanel(BorderLayout())
  panel2 = JPanel(FlowLayout())
  panel2.add(widget)
  panel.add("North", panel2)
  panel.add("Center",disp.getComponent())

  frame = JFrame(title)
  pane = frame.getContentPane()
  pane.add(panel)

  frame.setSize(400,550)
  frame.setVisible(1)

  return

