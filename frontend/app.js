const API = 'http://127.0.0.1:8080/api';
const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

let token = '';

async function req(path, options = {}) {
  const headers = {'Content-Type':'application/json', ...(options.headers || {})};
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${API}${path}`, { ...options, headers });
  if (!res.ok) throw new Error(await res.text());
  if (res.status === 204) return null;
  return res.json();
}

function toast(msg) {
  $('#status').textContent = msg;
}

function renderBars(containerId, rows) {
  const wrap = $(containerId);
  if (!wrap) return;
  if (!rows.length) {
    wrap.innerHTML = '<p style="color:#9fb0d2;">暂无数据</p>';
    return;
  }
  const max = Math.max(...rows.map(r => r.value), 1);
  wrap.innerHTML = rows.map(r => `
    <div class="bar-row">
      <span class="bar-label">${r.label}</span>
      <div class="bar-track"><span class="bar-fill" style="width:${(r.value / max) * 100}%"></span></div>
      <span class="bar-value">${r.value}</span>
    </div>
  `).join('');
}

function renderStatsPanel(stats) {
  $('#statKpis').innerHTML = [
    ['题库题目数', stats.questionCount],
    ['试卷总数', stats.paperCount],
    ['平行组数量', stats.parallelGroupCount],
    ['发放记录数', stats.assignmentCount]
  ].map(([k, v]) => `<div class="card"><h3>${v}</h3><p>${k}</p></div>`).join('');

  const typeRows = Object.entries(stats.questionTypeDistribution || {}).map(([k, v]) => ({ label: k, value: v }));
  renderBars('#typeBars', typeRows);

  const knowledge = stats.topKnowledgeCoverage || [];
  $('#knowledgeTable tbody').innerHTML = knowledge.length
    ? knowledge.map(x => `<tr><td>${x.knowledgePoint}</td><td>${x.count}</td></tr>`).join('')
    : '<tr><td colspan="2">暂无数据</td></tr>';

  const paperRows = Object.entries(stats.paperAssignmentDistribution || {}).map(([k, v]) => ({ label: k, value: v }));
  renderBars('#paperBars', paperRows);
}

function renderTodo(todo) {
  $('#todoKpis').innerHTML = [
    ['待开始考试', todo.draftLikeExamCount],
    ['进行中考试', todo.ongoingExamCount],
    ['已结束考试', todo.finishedExamCount],
    ['待主观题复核', todo.pendingManualReviewCount]
  ].map(([k,v]) => `<div class="card"><h3>${v}</h3><p>${k}</p></div>`).join('');
}

async function downloadSubmissionsCsv(examId) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  const res = await fetch(`${API}/teacher/exams/${examId}/submissions/export`, { headers });
  if (!res.ok) throw new Error(await res.text());
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `exam-${examId}-submissions.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function renderSubmissionDetail(detail) {
  const container = $('#submissionDetail');
  if (!detail || !detail.answers || !detail.answers.length) {
    container.innerHTML = '该提交暂无逐题数据。';
    return;
  }

  const rows = detail.answers.map(a => `
    <tr>
      <td>${a.questionId}</td>
      <td>${a.questionType}</td>
      <td>${a.stem}</td>
      <td>${a.studentAnswer || '-'}</td>
      <td>${a.referenceAnswer || '-'}</td>
      <td>${a.objective ? (a.answerMatched ? '✅' : '❌') : '人工判分'}</td>
    </tr>
  `).join('');

  container.innerHTML = `
    <div class="card">
      <h4>提交详情：${detail.studentName} / ${detail.examTitle}</h4>
      <p style="color:#9fb0d2;">提交时间：${detail.submittedAt}</p>
      <table class="table">
        <thead><tr><th>题目ID</th><th>题型</th><th>题干</th><th>学生答案</th><th>参考答案</th><th>判定</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
  `;
}

async function login() {
  try {
    const data = await req('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: $('#username').value, password: $('#password').value })
    });
    token = data.token;
    $('#loginBox').style.display = 'none';
    await Promise.all([refresh(), loadQuestions(), loadExams()]);
  } catch (e) {
    $('#loginMsg').textContent = '登录失败，请检查账号密码';
  }
}

async function refresh() {
  try {
    const [stats, todo] = await Promise.all([req('/teacher/statistics'), req('/teacher/todo')]);
    $('#status').textContent = 'API: 已连接（已登录）';
    $('#kpis').innerHTML = [
      ['题库总量', stats.questionCount],
      ['试卷总量', stats.paperCount],
      ['平行组', stats.parallelGroupCount],
      ['发放记录', stats.assignmentCount]
    ].map(([k,v])=>`<div class="card"><h3>${v}</h3><p>${k}</p></div>`).join('');
    $('#statsJson').textContent = JSON.stringify(stats, null, 2);
    renderStatsPanel(stats);
    renderTodo(todo);
  } catch (e) {
    $('#status').textContent = 'API: 未连接';
    $('#statsJson').textContent = String(e);
  }
}

async function loadQuestions() {
  const type = $('#filterType').value;
  const keyword = $('#filterKeyword').value;
  const query = new URLSearchParams();
  if (type) query.set('type', type);
  if (keyword) query.set('keyword', keyword);
  const list = await req(`/teacher/questions?${query.toString()}`);
  $('#questionTable tbody').innerHTML = list.map(q => `<tr><td>${q.id}</td><td>${q.type}</td><td>${q.stem}</td><td>${(q.knowledgePoints || []).join(', ')}</td><td><button data-edit="${q.id}">编辑</button><button data-del="${q.id}">删除</button></td></tr>`).join('');

  $$('#questionTable [data-del]').forEach(btn => btn.onclick = async () => {
    await req(`/teacher/questions/${btn.dataset.del}`, { method: 'DELETE' });
    await loadQuestions();
    await refresh();
    toast('题目已删除');
  });

  $$('#questionTable [data-edit]').forEach(btn => btn.onclick = async () => {
    const id = Number(btn.dataset.edit);
    const current = list.find(q => q.id === id);
    const stem = prompt('编辑题干', current.stem);
    if (!stem) return;
    const answer = prompt('编辑答案', current.answer);
    if (!answer) return;
    const know = prompt('编辑知识点(逗号分隔)', (current.knowledgePoints || []).join(','));
    await req(`/teacher/questions/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        type: current.type,
        stem,
        answer,
        options: current.options || [],
        knowledgePoints: (know || '').split(',').map(v => v.trim()).filter(Boolean)
      })
    });
    await loadQuestions();
    toast('题目已更新');
  });
}

async function loadExams() {
  const list = await req('/teacher/exams');
  $('#examTable tbody').innerHTML = list.map(e => `<tr><td>${e.id}</td><td>${e.title}</td><td>${e.status}</td><td>${e.paperId}</td><td><button data-sub="${e.id}">查看提交</button><button data-exp="${e.id}">导出CSV</button></td></tr>`).join('');

  $$('#examTable [data-sub]').forEach(btn => btn.onclick = async () => {
    const examId = btn.dataset.sub;
    const subs = await req(`/teacher/exams/${examId}/submissions`);
    $('#submissionTable tbody').innerHTML = subs.map(s => `<tr>
      <td>${s.studentName}</td>
      <td>${s.objectiveScore}</td>
      <td>${s.manualScore ?? '-'}</td>
      <td>${s.totalScore}</td>
      <td>${s.reviewStatus}</td>
      <td>${s.submittedAt}</td>
      <td><button data-detail="${s.id}">查看详情</button><button data-review="${s.id}">人工评分</button></td>
    </tr>`).join('');

    $$('#submissionTable [data-detail]').forEach(detailBtn => detailBtn.onclick = async () => {
      const sid = Number(detailBtn.dataset.detail);
      const detail = await req(`/teacher/submissions/${sid}`);
      renderSubmissionDetail(detail);
    });

    $$('#submissionTable [data-review]').forEach(reviewBtn => reviewBtn.onclick = async () => {
      const sid = Number(reviewBtn.dataset.review);
      const score = prompt('请输入主观题分数（>=0）', '0');
      if (score === null) return;
      await req(`/teacher/submissions/${sid}/manual-review`, {
        method: 'POST',
        body: JSON.stringify({ manualScore: Number(score) })
      });
      await btn.onclick();
      await refresh();
      toast(`提交 ${sid} 人工评分已保存`);
    });

    toast(`已加载考试 ${examId} 的提交记录`);
  });

  $$('#examTable [data-exp]').forEach(btn => btn.onclick = async () => {
    const examId = btn.dataset.exp;
    await downloadSubmissionsCsv(examId);
    toast(`考试 ${examId} 提交记录导出完成`);
  });
}

$$('.sidebar a').forEach(a => a.onclick = (e) => {
  e.preventDefault();
  $$('.sidebar a').forEach(x => x.classList.remove('active'));
  a.classList.add('active');
  $$('.panel').forEach(p => p.classList.remove('active'));
  $('#' + a.dataset.tab).classList.add('active');
});

$('#loginBtn').onclick = login;

$('#addQuestion').onclick = async () => {
  const body = {
    type: $('#qType').value,
    stem: $('#qStem').value,
    answer: $('#qAnswer').value,
    knowledgePoints: $('#qKnowledge').value.split(',').map(v => v.trim()).filter(Boolean),
    options: $('#qType').value === 'SINGLE_CHOICE' ? $('#qOptions').value.split('\n').map(v => v.trim()).filter(Boolean) : []
  };
  await req('/teacher/questions', {method: 'POST', body: JSON.stringify(body)});
  await loadQuestions();
  await refresh();
  toast('题目保存成功');
};

$('#createManual').onclick = async () => {
  const body = { title: $('#manualTitle').value, questionIds: $('#manualIds').value.split(',').map(v => Number(v.trim())).filter(Boolean) };
  await req('/teacher/papers/manual', {method: 'POST', body: JSON.stringify(body)});
  await refresh();
  toast('手动试卷创建成功');
};

$('#createRandom').onclick = async () => {
  const body = { title: $('#randomTitle').value, singleChoiceCount: Number($('#rc1').value), trueFalseCount: Number($('#rc2').value), shortAnswerCount: Number($('#rc3').value) };
  await req('/teacher/papers/random', {method: 'POST', body: JSON.stringify(body)});
  await refresh();
  toast('随机试卷创建成功');
};

$('#createGroup').onclick = async () => {
  const body = {
    name: $('#groupName').value,
    paperIds: $('#groupPaperIds').value.split(',').map(v => Number(v.trim())).filter(Boolean)
  };
  await req('/teacher/parallel-groups', { method: 'POST', body: JSON.stringify(body) });
  await refresh();
  toast('平行组创建成功');
};

$('#assignGroup').onclick = async () => {
  const groupId = $('#assignGroupId').value.trim();
  const studentNames = $('#assignStudents').value.split(',').map(v => v.trim()).filter(Boolean);
  await req(`/teacher/parallel-groups/${groupId}/assign`, { method: 'POST', body: JSON.stringify({ studentNames }) });
  await refresh();
  toast('平行组发放完成');
};

$('#createExam').onclick = async () => {
  const body = {
    paperId: Number($('#examPaperId').value),
    title: $('#examTitle').value,
    startTime: $('#examStart').value,
    endTime: $('#examEnd').value
  };
  await req('/teacher/exams', {method: 'POST', body: JSON.stringify(body)});
  await loadExams();
  toast('考试发布成功');
};

$('#loadQuestions').onclick = loadQuestions;
$('#loadExams').onclick = loadExams;
