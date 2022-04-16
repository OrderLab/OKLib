from lxml import etree
import subprocess

with open('pom.xml') as f:
	content = f.read()
	root = etree.fromstring(content)
	results = []
	count = 0
	for element in root.iter():
		if 'plugin' in str(element.tag) and 'plugins' not in str(element.tag):
			s = ''.join(element.itertext())
			if "findbugs-maven-plugin" in s and 'maven-compiler-plugin' not in s:
				print count
				count += 1
				print s
				results.append(element)
		if 'module' in str(element.tag) and 'modules' not in str(element.tag):
			s = ''.join(element.itertext())
			if "archetypes" in s:
				print count
				count += 1
				print s
				results.append(element)


	for bad in results:
		bad.getparent().remove(bad)    

	with open('pom2.xml','w') as f:
		f.write(etree.tostring(root, encoding='UTF-8', pretty_print=True, xml_declaration=True))
		print "done"


