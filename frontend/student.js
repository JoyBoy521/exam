const API = 'http://127.0.0.1:8080/api';
const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

let currentExam = null;

async function req(path, options = {}) {
  const res = await fetch(`${API}${path}`, {
    headers: {'Content-Type':'application/json'},
    ...options
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

$$('.sidebar a').forEach(a=>a.onclick=(e)=>{e.preventDefault();$$('.sidebar a').forEach(x=>x.classList.remove('active'));a.classList.add('active');$$('.panel').forEach(p=>p.classList.remove('active'));$('#'+a.dataset.tab).classList.add('active');});

$('#loadExamList').onclick = async () => {
  const studentName = $('#studentName').value.trim();
  const exams = await req(`/student/exams?studentName=${encodeURIComponent(studentName)}`);
  $('#studentStatus').textContent = `已加载 ${exams.length} 场考试`;
  $('#studentExamTable tbody').innerHTML = exams.map(e => `<tr><td>${e.id}</td><td>${e.title}</td><td>${e.status}</td><td><button data-use="${e.id}">去作答</button></td></tr>`).join('');
  $$('#studentExamTable [data-use]').forEach(btn => btn.onclick = () => {
    $('#takeExamId').value = btn.dataset.use;
    $$('.sidebar a').forEach(x=>x.classList.remove('active'));
    document.querySelector('[data-tab="take-exam"]').classList.add('active');
    $$('.panel').forEach(p=>p.classList.remove('active'));
    $('#take-exam').classList.add('active');
  });
};

$('#loadExamDetail').onclick = async () => {
  const examId = $('#takeExamId').value.trim();
  if (!examId) return;
  const detail = await req(`/student/exams/${examId}`);
  currentExam = detail;
  $('#questionContainer').innerHTML = detail.questions.map((q, idx) => {
    const options = (q.options || []).map(opt => `<label><input type="radio" name="q_${q.id}" value="${opt.split('.')[0] || opt}"> ${opt}</label>`).join('<br/>');
    const inputArea = q.type === 'SHORT_ANSWER'
      ? `<textarea data-q="${q.id}" placeholder="请输入简答"></textarea>`
      : `<div>${options}</div>`;
    return `<div class="card"><h4>第${idx+1}题 [${q.type}]</h4><p>${q.stem}</p>${inputArea}</div>`;
  }).join('');
};

$('#submitExam').onclick = async () => {
  if (!currentExam) return;
  const studentName = $('#studentName').value.trim();
  const answers = {};
  currentExam.questions.forEach(q => {
    if (q.type === 'SHORT_ANSWER') {
      const ta = document.querySelector(`textarea[data-q="${q.id}"]`);
      answers[q.id] = ta ? ta.value : '';
    } else {
      const checked = document.querySelector(`input[name="q_${q.id}"]:checked`);
      answers[q.id] = checked ? checked.value : '';
    }
  });
  const result = await req(`/student/exams/${currentExam.examId}/submit`, {
    method: 'POST',
    body: JSON.stringify({ studentName, answers })
  });
  $('#submitResult').textContent = JSON.stringify(result, null, 2);
};
