const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../src/main/java');
const known = {
  'jakarta.persistence': 'Entity Table UniqueConstraint Id GeneratedValue GenerationType ManyToOne OneToOne FetchType JoinColumn Version Column EnumType Enumerated EntityManager LockModeType PersistenceContext Index',
  'jakarta.validation.constraints': 'NotNull Positive Min Max DecimalMin Digits NotBlank Size Pattern',
  'java.util': 'List Map Set UUID ArrayList HashMap HashSet LinkedHashMap LinkedHashSet TreeMap TreeSet Arrays Comparator Objects Collections Locale Optional',
  'org.springframework.web.bind.annotation': 'RestController RequestMapping GetMapping PostMapping PutMapping RequestBody RequestParam PathVariable'
};
function walk(dir) {
  for (const e of fs.readdirSync(dir, {withFileTypes:true})) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith('.java')) {
      const original = fs.readFileSync(p, 'utf8');
      const next = original.replace(/^import ([\w.]+)\.\*;\r?$/gm, (_, pkg) => {
        const names = known[pkg]?.split(' ') ?? fs.readdirSync(path.join(root,...pkg.split('.'))).filter(f=>f.endsWith('.java')).map(f=>f.slice(0,-5));
        return names.filter(n=>new RegExp('\\b'+n+'\\b').test(original.replace(/^import .*$/gm,''))).map(n=>'import '+pkg+'.'+n+';').join('\n');
      });
      if (next !== original) fs.writeFileSync(p,next,'utf8');
    }
  }
}
walk(root);
