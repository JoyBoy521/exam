const STORAGE_KEY = 'exam-prototype-teacher-data-v3';

const defaultData = {
  knowledges: ['数组', '链表', '二叉树', '数据库索引'],
  questions: [],
  manualPapers: [],
  randomPapers: [],
  parallelGroups: [],
  assignments: []
};

const state = loadState();

function loadState() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return structuredClone(defaultData);
  try {
    return { ...structuredClone(defaultData), ...JSON.parse(raw) };
  } catch {
    return structuredClone(defaultData);
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function byId(id) {
  return document.getElementById(id);
}

function switchView(view) {
  document.querySelectorAll('.nav a').forEach((el) => {
    el.classList.toggle('active', el.dataset.view === view);
  });
  document.querySelectorAll('.view').forEach((el) => {
    el.classList.toggle('active', el.id === `${view}-view`);
  });
}

function switchExamTab(tab) {
  document.querySelectorAll('.tab-btn').forEach((el) => {
    el.classList.toggle('active', el.dataset.examTab === tab);
  });
  document.querySelectorAll('.exam-tab').forEach((el) => {
    el.classList.toggle('active', el.id === `exam-tab-${tab}`);
  });
}

function renderKnowledgeTags() {
  const wrap = byId('knowledge-tags');
  wrap.innerHTML = '';
  state.knowledges.forEach((k) => {
    const label = document.createElement('label');
    label.className = 'check-tag';
    label.innerHTML = `<input type="checkbox" value="${k}"/> ${k}`;
    wrap.appendChild(label);
  });
}

function getQuestionTypeStats(questionIds) {
  const bucket = { 选择题: 0, 判断题: 0, 简答题: 0 };
  questionIds.forEach((id) => {
    const q = state.questions.find((item) => item.id === id);
    if (q) bucket[q.type] += 1;
  });
  return `选择 ${bucket['选择题']} / 判断 ${bucket['判断题']} / 简答 ${bucket['简答题']}`;
}

function renderQuestionTable() {
  const tbody = byId('question-table').querySelector('tbody');
  tbody.innerHTML = '';
  state.questions.forEach((q) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${q.id}</td><td>${q.type}</td><td>${q.stem}</td><td>${q.knowledge.join('、') || '-'}</td><td>${q.answer}</td>`;
    tbody.appendChild(tr);
  });
}

function renderManualPicker() {
  const wrap = byId('manual-question-picker');
  wrap.innerHTML = '';
  state.questions.forEach((q) => {
    const label = document.createElement('label');
    label.className = 'picker-item';
    label.innerHTML = `<input type="checkbox" value="${q.id}" class="manual-pick"/> [${q.type}] ${q.stem}`;
    wrap.appendChild(label);
  });
}

function renderManualPapers() {
  const tbody = byId('manual-paper-table').querySelector('tbody');
  tbody.innerHTML = '';
  state.manualPapers.forEach((p) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${p.title}</td><td>${p.questionIds.length}</td><td>${getQuestionTypeStats(p.questionIds)}</td>`;
    tbody.appendChild(tr);
  });
}

function renderRandomPapers() {
  const tbody = byId('random-paper-table').querySelector('tbody');
  tbody.innerHTML = '';
  state.randomPapers.forEach((p) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${p.title}</td><td>${p.questionIds.length}</td><td>${getQuestionTypeStats(p.questionIds)}</td>`;
    tbody.appendChild(tr);
  });
}

function renderParallelPicker() {
  const wrap = byId('parallel-paper-picker');
  wrap.innerHTML = '';
  const allPapers = [
    ...state.manualPapers.map((p) => ({ id: `manual-${p.id}`, title: `[手动] ${p.title}` })),
    ...state.randomPapers.map((p) => ({ id: `random-${p.id}`, title: `[随机] ${p.title}` }))
  ];

  allPapers.forEach((p) => {
    const label = document.createElement('label');
    label.className = 'picker-item';
    label.innerHTML = `<input type="checkbox" value="${p.id}" class="parallel-pick"/> ${p.title}`;
    wrap.appendChild(label);
  });
}

function renderParallelSelect() {
  const select = byId('parallel-group-select');
  select.innerHTML = '';
  state.parallelGroups.forEach((g) => {
    const opt = document.createElement('option');
    opt.value = g.id;
    opt.textContent = `${g.name}（${g.paperRefs.length} 套卷）`;
    select.appendChild(opt);
  });
}

function renderAssignments() {
  const tbody = byId('assignment-table').querySelector('tbody');
  tbody.innerHTML = '';
  state.assignments.forEach((a) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${a.student}</td><td>${a.paperTitle}</td>`;
    tbody.appendChild(tr);
  });
}

function updateMetrics() {
  byId('metric-total').textContent = state.questions.length;
  byId('metric-manual').textContent = state.manualPapers.length;
  byId('metric-random').textContent = state.randomPapers.length;
  byId('metric-parallel').textContent = state.parallelGroups.length;
}

function renderStatistics() {
  const totalPapers = state.manualPapers.length + state.randomPapers.length;
  byId('stat-q-total').textContent = state.questions.length;
  byId('stat-paper-total').textContent = totalPapers;
  byId('stat-assignment-total').textContent = state.assignments.length;
  byId('stat-knowledge-total').textContent = state.knowledges.length;

  const typeBuckets = { 选择题: 0, 判断题: 0, 简答题: 0 };
  state.questions.forEach((q) => {
    typeBuckets[q.type] += 1;
  });
  const typeTbody = byId('type-stat-table').querySelector('tbody');
  typeTbody.innerHTML = '';
  const total = state.questions.length || 1;
  Object.entries(typeBuckets).forEach(([type, count]) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${type}</td><td>${count}</td><td>${((count / total) * 100).toFixed(1)}%</td>`;
    typeTbody.appendChild(tr);
  });

  const knowledgeBucket = new Map();
  state.questions.forEach((q) => {
    q.knowledge.forEach((k) => knowledgeBucket.set(k, (knowledgeBucket.get(k) || 0) + 1));
  });
  const knowledgeRows = [...knowledgeBucket.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5);
  const knowledgeTbody = byId('knowledge-stat-table').querySelector('tbody');
  knowledgeTbody.innerHTML = '';
  if (knowledgeRows.length === 0) {
    knowledgeTbody.innerHTML = '<tr><td colspan="2">暂无数据</td></tr>';
  } else {
    knowledgeRows.forEach(([k, c]) => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${k}</td><td>${c}</td>`;
      knowledgeTbody.appendChild(tr);
    });
  }

  const paperDistribution = new Map();
  state.assignments.forEach((a) => {
    paperDistribution.set(a.paperTitle, (paperDistribution.get(a.paperTitle) || 0) + 1);
  });
  const distTbody = byId('paper-distribution-table').querySelector('tbody');
  distTbody.innerHTML = '';
  if (paperDistribution.size === 0) {
    distTbody.innerHTML = '<tr><td colspan="2">暂无发放记录</td></tr>';
  } else {
    [...paperDistribution.entries()].forEach(([paper, cnt]) => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${paper}</td><td>${cnt}</td>`;
      distTbody.appendChild(tr);
    });
  }
}

function sample(arr, count) {
  const temp = [...arr];
  const result = [];
  for (let i = 0; i < count && temp.length > 0; i += 1) {
    const idx = Math.floor(Math.random() * temp.length);
    result.push(temp[idx]);
    temp.splice(idx, 1);
  }
  return result;
}

function refreshAll() {
  renderKnowledgeTags();
  renderQuestionTable();
  renderManualPicker();
  renderManualPapers();
  renderRandomPapers();
  renderParallelPicker();
  renderParallelSelect();
  renderAssignments();
  updateMetrics();
  renderStatistics();
}

document.querySelectorAll('.nav a').forEach((el) => {
  el.addEventListener('click', (e) => {
    e.preventDefault();
    switchView(el.dataset.view);
  });
});

document.querySelectorAll('.tab-btn').forEach((el) => {
  el.addEventListener('click', () => switchExamTab(el.dataset.examTab));
});

byId('add-knowledge-btn').addEventListener('click', () => {
  const val = byId('new-knowledge').value.trim();
  if (!val) return;
  if (!state.knowledges.includes(val)) {
    state.knowledges.push(val);
    saveState();
    renderKnowledgeTags();
    renderStatistics();
  }
  byId('new-knowledge').value = '';
});

byId('add-question-btn').addEventListener('click', () => {
  const type = byId('q-type').value;
  const stem = byId('q-stem').value.trim();
  const answer = byId('q-answer').value.trim();
  const options = byId('q-options').value.trim();
  const knowledge = Array.from(document.querySelectorAll('#knowledge-tags input:checked')).map((x) => x.value);

  if (!stem || !answer) {
    alert('题干和答案不能为空');
    return;
  }

  const q = { id: `Q${Date.now()}`, type, stem, answer, options, knowledge };
  state.questions.unshift(q);
  saveState();
  refreshAll();

  byId('q-stem').value = '';
  byId('q-answer').value = '';
  byId('q-options').value = '';
  document.querySelectorAll('#knowledge-tags input').forEach((x) => { x.checked = false; });
});

document.addEventListener('change', (e) => {
  if (!e.target.classList.contains('manual-pick')) return;
  const checked = document.querySelectorAll('.manual-pick:checked').length;
  byId('manual-count').textContent = `已选 ${checked} 题`;
});

byId('create-manual-paper-btn').addEventListener('click', () => {
  const title = byId('manual-paper-title').value.trim();
  const selected = Array.from(document.querySelectorAll('.manual-pick:checked')).map((x) => x.value);
  if (!title || selected.length === 0) {
    alert('请输入试卷名并至少选择 1 题');
    return;
  }
  state.manualPapers.unshift({ id: Date.now(), title, questionIds: selected });
  saveState();
  refreshAll();
  byId('manual-paper-title').value = '';
  byId('manual-count').textContent = '已选 0 题';
});

byId('generate-random-btn').addEventListener('click', () => {
  const title = byId('random-title').value.trim() || `随机卷-${state.randomPapers.length + 1}`;
  const selectCnt = Number(byId('random-select').value || 0);
  const judgeCnt = Number(byId('random-judge').value || 0);
  const shortCnt = Number(byId('random-short').value || 0);

  const poolSelect = state.questions.filter((q) => q.type === '选择题');
  const poolJudge = state.questions.filter((q) => q.type === '判断题');
  const poolShort = state.questions.filter((q) => q.type === '简答题');

  if (poolSelect.length < selectCnt || poolJudge.length < judgeCnt || poolShort.length < shortCnt) {
    alert('题库数量不足，无法满足随机组卷条件');
    return;
  }

  const picked = [...sample(poolSelect, selectCnt), ...sample(poolJudge, judgeCnt), ...sample(poolShort, shortCnt)];
  state.randomPapers.unshift({ id: Date.now(), title, questionIds: picked.map((q) => q.id) });
  saveState();
  refreshAll();
});

byId('create-parallel-group-btn').addEventListener('click', () => {
  const name = byId('parallel-group-name').value.trim();
  const paperRefs = Array.from(document.querySelectorAll('.parallel-pick:checked')).map((x) => x.value);
  if (!name || paperRefs.length < 2) {
    alert('请填写组名，并至少选择 2 套试卷用于平行发放');
    return;
  }
  state.parallelGroups.unshift({ id: `PG${Date.now()}`, name, paperRefs });
  saveState();
  refreshAll();
  byId('parallel-group-name').value = '';
});

byId('assign-parallel-btn').addEventListener('click', () => {
  const groupId = byId('parallel-group-select').value;
  const group = state.parallelGroups.find((g) => g.id === groupId);
  const students = byId('student-list').value.split('\n').map((s) => s.trim()).filter(Boolean);

  if (!group || students.length === 0) {
    alert('请先选择平行组并填写学生名单');
    return;
  }

  const paperTitleMap = new Map();
  state.manualPapers.forEach((p) => paperTitleMap.set(`manual-${p.id}`, `[手动] ${p.title}`));
  state.randomPapers.forEach((p) => paperTitleMap.set(`random-${p.id}`, `[随机] ${p.title}`));

  state.assignments = students.map((student) => {
    const ref = group.paperRefs[Math.floor(Math.random() * group.paperRefs.length)];
    return { student, paperTitle: paperTitleMap.get(ref) || ref };
  });

  saveState();
  refreshAll();
});

switchExamTab('manual');
refreshAll();
