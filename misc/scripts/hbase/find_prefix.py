import os.path

file = open('all_test_method_names.txt','r')
lines = file.readlines()
count = 0
for i in range(len(lines)-2):
    lst = [lines[i],lines[i+1], lines[i+2]]
    if len(os.path.commonprefix(lst))>15:
        print count
	print i, lines[i]
        print i+1, lines[i+1]
        print i+2, lines[i+2]
	count += 1
