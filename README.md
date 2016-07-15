# hsit-generation

this generates text from abductive interpretations.
Includes the [TriangleCOPA](https://github.com/asgordon/TriangleCOPA) interpretations.

Main is in the class nlg3.COPANLG3

If using the nlg3.selection.ParserSelection, you will need the [BLLIP parser](https://github.com/BLLIP/bllip-parser) installed. You also need to change charniak.CharniakParser.start() to your specific environment.
After that, you should start the parser running charniak.Server and wait some tiem for it to be ready to receive requests (i.e. load the models).

to cite use the paper:
Emily Ahn, Fabrizio Morbini, Andrew Gordon. Improving Fluency in Narrative Text Generation With Grammatical Transformations and Probabilistic Parsing. INLG 2016.
