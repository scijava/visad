from visad.python.JPythonMethods import *
area = read("../examples/AREA2001")
print area.length

# make a scratch across the top of the area
for i in range(20000, 21000):
	area[i] = 0

map = read("../examples/OUTLSUPW")
print map.length

# print area pixel values at some map locations
for j in range(map.length/200):
	i = 200 * j
	print "area = ", area[map[i]], " at ", map[i]

plot(area)
plot(map)
