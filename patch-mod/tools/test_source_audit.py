"""Focused checks for the source comparison reader (stdlib only)."""
import importlib.util, pathlib, tempfile, unittest, json, struct, zipfile
spec=importlib.util.spec_from_file_location('source_audit',pathlib.Path(__file__).with_name('source-audit.py'))
audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)
class SourceAuditTest(unittest.TestCase):
    def test_java_literals_and_modified_utf8(self):
        raw=b'hi\xc0\x80'+bytes.fromhex('eda0bdedb880')
        data=bytes.fromhex('cafebabe0000003d')+struct.pack('>H',3)+b'\x01'+struct.pack('>H',len(raw))+raw+b'\x08\x00\x01'
        self.assertEqual(['hi\0\U0001f600'],audit.class_strings(data))
    def test_json_field_and_prefix_selection(self):
        with tempfile.TemporaryDirectory() as folder:
            p=pathlib.Path(folder)/'input.json';p.write_text(json.dumps({'x':[{'id':'the_vault:overworld/a','name':'Ignored'},{'id':'other:a'}]}))
            values=audit.read_source({'path':str(p),'fields':['id'],'prefixes':['the_vault:overworld/']})
            self.assertEqual({'$.x[0].id':'the_vault:overworld/a'},values['entries'])
            before=values['sha256'];p.write_text(p.read_text()+' ')
            self.assertNotEqual(before,audit.read_source({'path':str(p)})['sha256'])
    def test_deleted_class_is_reportable(self):
        with tempfile.TemporaryDirectory() as folder:
            p=pathlib.Path(folder)/'input.jar'
            with zipfile.ZipFile(p,'w'):pass
            result=audit.read_source({'path':str(p),'classes':['removed/Class']})
            self.assertEqual({'removed/Class':None},result['entries']);self.assertEqual({'removed/Class':[]},result['texts'])
if __name__=='__main__':unittest.main()
