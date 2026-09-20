import json, pathlib, sys
root=pathlib.Path(sys.argv[1]); required=int(sys.argv[2]); passed=True
lines=['# Two-client acceptance report','']
for role in ['A','B']:
    p=root/f'client-{role}.jsonl'
    events=[]; malformed=False
    for line in p.read_text().splitlines() if p.exists() else []:
        try: events.append(json.loads(line))
        except json.JSONDecodeError: malformed=True
    summary=root/f'client-{role}.summary'
    data=dict(line.split('=',1) for line in summary.read_text().splitlines() if '=' in line) if summary.exists() else {}
    completed=sum(e['type']=='MATCH_COMPLETE' for e in events)
    relogin=any(e['type']=='RELOGIN_CONFIRMED' for e in events)
    ok=not malformed and data.get('status')=='DONE' and completed==required and relogin
    passed &= ok
    lines += [f'## Client {role}',f'- Result: {"PASS" if ok else "FAIL / INCOMPLETE"}',f'- Server-confirmed matches: {completed}/{required}',f'- Relogin confirmed: {relogin}',f'- Broadcasts received: {sum(e["type"]=="BROADCAST" for e in events)}','']
lines += ['Admin/SQL data comparison requires the separate server-side acceptance runner; it is not inferred from completing matches.']
(root/'summary.md').write_text('\n'.join(lines)+'\n')
print('\n'.join(lines));sys.exit(0 if passed else 1)
