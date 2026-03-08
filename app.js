"use strict";
// CO5: "use strict" – security best practice

// ── CO3: Number literals ──
const MAX_SLOTS = 0x3C;     // hex literal = 60
const TOAST_MS  = 4e3;      // scientific literal = 4000

// ── CO4: DOM shorthand helpers (arrow functions) ──
const $ = id  => document.getElementById(id);
const $q = sel => document.querySelector(sel);

// ════════════════════════════════════════════
// CO3: Classes + Object Inheritance
// ════════════════════════════════════════════
class DataStore {
  constructor() { this._s = {}; }
  set(k, v)  { this._s[String(k)] = v; }
  get(k)     { return this._s[String(k)]; }
  has(k)     { return String(k) in this._s; }
  delete(k)  { delete this._s[String(k)]; }
  getAll()   { return Object.values(this._s); }
}

// CO3: extends + super
class UserStore extends DataStore {
  constructor(role) { super(); this.role = role; }
  findByEmail(email) {
    // CO3: Array method – find (like filter for one result)
    return this.getAll().find(u => u.email === email) || null;
  }
}

// CO3: Queue class
class Queue {
  #items = [];
  enqueue(item) { this.#items.push(item); }
  dequeue()     { return this.#items.shift(); }
  get size()    { return this.#items.length; }
  getAll()      { return [...this.#items]; }
}

// ── Initialise stores ──
const adminStore    = new UserStore("admin");
const customerStore = new UserStore("customer");
const parkingBasements = { 1: new DataStore(), 2: new DataStore(), 3: new DataStore() };
const notifQueue    = new Queue();
const serviceRequests = [];   // CO3: Array

// ── Session state ──
let currentUser    = { type: null, data: null };
let currentBasement = 1;
let selectedSlot    = null;

// ════════════════════════════════════════════
// CO4: localStorage wrapper (setItem/getItem)
// ════════════════════════════════════════════
const Storage = {
  save(key, val) {
    try { localStorage.setItem(key, JSON.stringify(val)); } catch(e) {}
  },
  load(key) {
    try { return JSON.parse(localStorage.getItem(key)); } catch(e) { return null; }
  }
};

// ════════════════════════════════════════════
// CO3: Initialise parking slots
// ════════════════════════════════════════════
function initParkingSlots() {
  // CO3: Array.forEach
  [1, 2, 3].forEach((b, i) => {
    const preOccupied = [45, 50, 43][i];
    initBasement(b, preOccupied);
  });
}

function initBasement(bNum, preOccupied) {
  const rows = ['A','B','C','D','E','F'];
  const allIds = [];

  // CO3: for…of loop
  for (const row of rows) {
    // CO3: for loop
    for (let n = 1; n <= 10; n++) {
      const id = `${n}${row}`;
      parkingBasements[bNum].set(id, {
        id, basement: bNum, status: 'free',
        occupiedBy: null, occupiedByName: null, timestamp: null
      });
      allIds.push(id);
    }
  }

  // CO3: Array sort (shuffle) + slice
  const shuffled = allIds.sort(() => Math.random() - 0.5);
  for (let i = 0; i < preOccupied; i++) {
    const slot = parkingBasements[bNum].get(shuffled[i]);
    Object.assign(slot, {
      status: 'occupied',
      occupiedBy: `PRE_${i}`,
      occupiedByName: `Customer ${i + 1}`,
      timestamp: new Date().toISOString()
    });
    parkingBasements[bNum].set(shuffled[i], slot);
  }
}

// CO3: Array.reduce to count slot statuses
function getBasementCounts(bNum) {
  return parkingBasements[bNum].getAll().reduce((acc, s) => {
    acc[s.status] = (acc[s.status] || 0) + 1;
    return acc;
  }, { free: 0, occupied: 0, leaving: 0 });
}

// ════════════════════════════════════════════
// UI helpers
// ════════════════════════════════════════════
function hideAll() {
  ['welcomeScreen','adminAuth','customerAuth','adminDashboard','customerDashboard']
    .forEach(id => $(id)?.classList.add('hidden'));
}
function goBack()                { hideAll(); $('welcomeScreen').classList.remove('hidden'); }
function selectUserType(type)    { hideAll(); $(type === 'admin' ? 'adminAuth' : 'customerAuth').classList.remove('hidden'); }
const showAdminSignup    = () => { $('adminLoginForm').classList.add('hidden');    $('adminSignupForm').classList.remove('hidden'); };
const showAdminLogin     = () => { $('adminSignupForm').classList.add('hidden');   $('adminLoginForm').classList.remove('hidden'); };
const showCustomerSignup = () => { $('customerLoginForm').classList.add('hidden'); $('customerSignupForm').classList.remove('hidden'); };
const showCustomerLogin  = () => { $('customerSignupForm').classList.add('hidden');$('customerLoginForm').classList.remove('hidden'); };

// ════════════════════════════════════════════
// CO4: Form validation helper
// ════════════════════════════════════════════
function validateFields(fields) {
  for (const { el, msg } of fields) {           // CO3: for…of
    if (!el || !el.value.trim()) {
      showToast(msg, 'error');
      el?.focus();
      return false;
    }
  }
  return true;
}

// ════════════════════════════════════════════
// Admin Auth
// ════════════════════════════════════════════
function adminLogin(e) {
  e.preventDefault();
  const email = $('adminLoginEmail').value.trim();
  const id    = $('adminLoginId').value.trim();
  if (!validateFields([
    { el: $('adminLoginEmail'), msg: 'Please enter your email' },
    { el: $('adminLoginId'),    msg: 'Please enter your Admin ID' }
  ])) return;

  // CO4: try/catch/throw
  try {
    const admin = adminStore.findByEmail(email);
    // CO3: ternary operator
    if (!admin)         throw new Error('Admin not found. Please sign up.');
    if (admin.id !== id) throw new Error('Incorrect Admin ID.');
    currentUser = { type: 'admin', data: admin };
    Storage.save('session', { type: 'admin', email });
    launchAdminDashboard();
  } catch(err) {
    showToast(err.message, 'error');
  }
}

function adminSignup(e) {
  e.preventDefault();
  const name  = $('adminSignupName').value.trim();
  const email = $('adminSignupEmail').value.trim();
  const phone = $('adminSignupPhone').value.trim();
  if (!validateFields([
    { el: $('adminSignupName'),  msg: 'Name is required' },
    { el: $('adminSignupEmail'), msg: 'Email is required' },
    { el: $('adminSignupPhone'), msg: 'Phone is required' }
  ])) return;
  if (adminStore.findByEmail(email)) { showToast('Email already registered.', 'error'); return; }
  // CO3: hex number + toString
  const id = 'ADM' + Math.floor(Math.random() * 0xFFFFF).toString().slice(0,5).padStart(5,'0');
  adminStore.set(email, { id, name, email, phone });
  showToast(`Account created! Your Admin ID: ${id}`, 'success');
  showAdminLogin();
}

// ════════════════════════════════════════════
// Customer Auth
// ════════════════════════════════════════════
function customerLogin(e) {
  e.preventDefault();
  const email = $('customerLoginEmail').value.trim();
  if (!validateFields([{ el: $('customerLoginEmail'), msg: 'Please enter your email' }])) return;
  try {
    const cust = customerStore.findByEmail(email);
    if (!cust) throw new Error('Customer not found. Please sign up.');
    currentUser = { type: 'customer', data: cust };
    Storage.save('session', { type: 'customer', email });
    launchCustomerDashboard();
  } catch(err) {
    showToast(err.message, 'error');
  }
}

function customerSignup(e) {
  e.preventDefault();
  const name    = $('customerSignupName').value.trim();
  const mall    = $('customerMallSelect').value;
  const email   = $('customerSignupEmail').value.trim();
  const phone   = $('customerSignupPhone').value.trim();
  const vehicle = $('vehicleNumber').value.trim().toUpperCase();

  if (!validateFields([
    { el: $('customerSignupName'),  msg: 'Name is required' },
    { el: $('customerSignupEmail'), msg: 'Email is required' },
    { el: $('customerSignupPhone'), msg: 'Phone is required' },
    { el: $('vehicleNumber'),       msg: 'Vehicle number is required' }
  ])) return;
  if (!mall)    { showToast('Please select a mall', 'error'); return; }
  if (customerStore.findByEmail(email)) { showToast('Email already registered.', 'error'); return; }

  const id = 'CUST' + String(Math.floor(Math.random() * 1e5)).padStart(5,'0');
  customerStore.set(email, {
    id, name, mall, email, phone, vehicle,
    currentSlot: null, currentBasement: null
  });
  // CO4: sessionStorage (setItem)
  sessionStorage.setItem('lastSignup', email);
  showToast('Account created! Please login.', 'success');
  showCustomerLogin();
}

// ════════════════════════════════════════════
// Dashboards
// ════════════════════════════════════════════
function launchAdminDashboard() {
  hideAll();
  $('adminDashboard').classList.remove('hidden');
  $('adminNameDisplay').textContent = `${currentUser.data.name} (${currentUser.data.id})`;
  currentBasement = 1;
  updateAllBasementInfo('admin');
  renderAdminGrid();
  renderAdminNotifications();
  renderCustomerRecords();
  renderAdminServiceRequests();
  setActiveBasementBtn('admin', 1);
}

function launchCustomerDashboard() {
  hideAll();
  $('customerDashboard').classList.remove('hidden');
  // CO4: DOM manipulation – textContent
  $('customerNameDisplay').textContent = currentUser.data.name;
  $('customerMallDisplay').textContent = `🏬 ${currentUser.data.mall}`;
  currentBasement = 1;
  updateAllBasementInfo('customer');
  renderCustomerGrid();
  updateBookingCard();
  renderMyServiceRequests();
  setActiveBasementBtn('customer', 1);
}

// ════════════════════════════════════════════
// Basement switching
// ════════════════════════════════════════════
function setActiveBasementBtn(role, num) {
  // CO4: querySelectorAll + forEach
  const allBtns = document.querySelectorAll(`#${role}Dashboard .basement-btn, #${role}Dashboard ~ * .basement-btn`);
  document.querySelectorAll('.basement-btn').forEach((btn, i) => {
    const matches = (Math.floor(i / 3) === (role === 'admin' ? 0 : 1) ? i % 3 : i) === num - 1;
  });
  // Simpler: target by basement-selector inside current visible section
  const section = $(role + 'Dashboard');
  if (!section) return;
  section.querySelectorAll('.basement-btn').forEach((btn, i) => {
    const active = (i === num - 1);
    btn.classList.toggle('active', active);
    btn.setAttribute('aria-pressed', String(active));
  });
}

function switchBasement(num, role) {
  currentBasement = num;
  setActiveBasementBtn(role, num);
  if (role === 'admin') {
    $('adminBasementTitle').textContent = `Basement ${num} – Parking Management`;
    renderAdminGrid();
  } else {
    $('customerBasementTitle').textContent = `Basement ${num} – Available Parking`;
    renderCustomerGrid();
  }
}

function updateAllBasementInfo(role) {
  [1,2,3].forEach(b => {
    const c = getBasementCounts(b);
    const prefix = role === 'admin' ? 'admin' : 'customer';
    $(prefix + 'B' + b + 'Info').textContent = `${c.free} free`;
  });
}

// ════════════════════════════════════════════
// CO3: Render grids (DOM manipulation)
// ════════════════════════════════════════════
function renderAdminGrid() {
  const grid = $('adminParkingGrid');
  grid.innerHTML = '';
  const rows = ['A','B','C','D','E','F'];
  // CO3: Array method – reduce for counts
  let counts = { free: 0, occupied: 0, leaving: 0 };

  for (const row of rows) {
    for (let n = 1; n <= 10; n++) {
      const id   = `${n}${row}`;
      const slot = parkingBasements[currentBasement].get(id);
      const el   = document.createElement('div');
      el.className = `parking-slot ${slot.status}`;
      el.setAttribute('role','gridcell');
      el.setAttribute('aria-label',`Slot ${id} – ${slot.status}`);
      // CO4: innerHTML
      el.innerHTML = `
        <div class="slot-icon">${slot.status==='free'?'🅿️':slot.status==='leaving'?'⏳':'🚗'}</div>
        <div class="slot-number">${slot.id}</div>
        ${slot.occupiedByName ? `<div class="slot-customer">${slot.occupiedByName}</div>` : ''}
      `;
      // CO4: click event
      if (slot.status === 'leaving') el.onclick = () => adminFreeSlot(id);
      grid.appendChild(el);
      counts[slot.status]++;
    }
  }
  // CO4: textContent DOM update
  $('adminFreeCount').textContent     = counts.free;
  $('adminOccupiedCount').textContent = counts.occupied;
  $('adminLeavingCount').textContent  = counts.leaving;
  updateAllBasementInfo('admin');
}

function adminFreeSlot(slotId) {
  const slot = parkingBasements[currentBasement].get(slotId);
  // CO3: Object spread
  parkingBasements[currentBasement].set(slotId, { ...slot, status:'free', occupiedBy:null, occupiedByName:null, timestamp:null });
  showToast(`Slot ${slotId} marked as free ✅`, 'success');
  renderAdminGrid();
}

function renderCustomerGrid() {
  const grid = $('customerParkingGrid');
  grid.innerHTML = '';
  const rows = ['A','B','C','D','E','F'];
  let counts = { free: 0, occupied: 0, leaving: 0 };

  for (const row of rows) {
    for (let n = 1; n <= 10; n++) {
      const id     = `${n}${row}`;
      const slot   = parkingBasements[currentBasement].get(id);
      const isMine = slot.occupiedBy === currentUser.data.email;
      const dispStatus = isMine ? 'leaving' : slot.status;
      const el = document.createElement('div');
      el.className = `parking-slot ${dispStatus}`;
      el.setAttribute('role','gridcell');
      el.setAttribute('aria-label',`Slot ${id} – ${dispStatus}`);
      el.innerHTML = `
        <div class="slot-icon">${isMine?'🚙':'🚗'}</div>
        <div class="slot-number">${slot.id}</div>
        ${isMine?'<div class="slot-customer">Your Slot</div>':''}
      `;
      if (slot.status === 'free' || isMine) el.onclick = () => customerSlotAction(id);
      grid.appendChild(el);
      counts[slot.status]++;
    }
  }
  $('customerFreeCount').textContent    = counts.free;
  $('customerOccupiedCount').textContent= counts.occupied;
  $('customerLeavingCount').textContent = counts.leaving;
  updateAllBasementInfo('customer');
  updateBookingCard();
}

// ════════════════════════════════════════════
// Live Booking Status Card
// ════════════════════════════════════════════
function updateBookingCard() {
  const cust = currentUser.data;
  const card = $('myBookingCard');
  const body = $('bookingStatusContent');
  if (cust.currentSlot) {
    card.classList.remove('hidden');
    // CO4: innerHTML reactive update
    body.innerHTML = `
      <div class="booking-badge">🅿️ ${cust.currentSlot}</div>
      <div>
        <p><strong>Basement ${cust.currentBasement}</strong></p>
        <p>Vehicle: ${cust.vehicle}</p>
        <p style="color:var(--text-secondary);font-size:.85rem">Click your slot on the grid to manage it</p>
      </div>
    `;
  } else {
    card.classList.add('hidden');
  }
}

// ════════════════════════════════════════════
// CO4: Modal – DOM events
// ════════════════════════════════════════════
function customerSlotAction(slotId) {
  const slot = parkingBasements[currentBasement].get(slotId);
  const cust = currentUser.data;
  selectedSlot = slotId;

  if (slot.status === 'free') {
    if (cust.currentSlot) { showToast('You already have a slot! Leave it first.', 'error'); return; }
    $('modalTitle').textContent    = 'Available Parking Slot';
    $('modalSlotNumber').textContent = slotId;
    $('modalMessage').textContent  = `Book parking slot ${slotId}?`;
    $('modalOccupyBtn').classList.remove('hidden');
    $('modalLeaveBtn').classList.add('hidden');
    $('modalLeavingSoonBtn').classList.add('hidden');
  } else if (slot.occupiedBy === cust.email) {
    $('modalTitle').textContent    = 'Your Parking Slot';
    $('modalSlotNumber').textContent = slotId;
    $('modalMessage').textContent  = 'Manage your slot:';
    $('modalOccupyBtn').classList.add('hidden');
    $('modalLeaveBtn').classList.remove('hidden');
    $('modalLeavingSoonBtn').classList.remove('hidden');
  } else {
    showToast(`Slot ${slotId} is not available`, 'error'); return;
  }
  $('slotModal').classList.add('active');
}

function closeModal() {
  $('slotModal').classList.remove('active');
  selectedSlot = null;
}

function confirmOccupy() {
  if (!selectedSlot) return;
  const slot = parkingBasements[currentBasement].get(selectedSlot);
  const cust = currentUser.data;
  parkingBasements[currentBasement].set(selectedSlot, {
    ...slot, status:'occupied', occupiedBy: cust.email,
    occupiedByName: cust.name, timestamp: new Date().toISOString()
  });
  cust.currentSlot = selectedSlot;
  cust.currentBasement = currentBasement;
  customerStore.set(cust.email, cust);
  currentUser.data = cust;
  Storage.save('session', { type:'customer', email: cust.email });

  addNotification(`🚗 ${cust.name} booked slot ${selectedSlot} in Basement ${currentBasement}`);
  renderCustomerRecords();
  showToast(`Slot ${selectedSlot} booked! Happy parking 🎉`, 'success');
  closeModal();
  renderCustomerGrid();
}

function confirmLeavingSoon() {
  if (!selectedSlot) return;
  const slot = parkingBasements[currentBasement].get(selectedSlot);
  parkingBasements[currentBasement].set(selectedSlot, { ...slot, status:'leaving' });
  addNotification(`⏰ ${currentUser.data.name} is leaving slot ${selectedSlot} soon`);
  showToast(`Slot ${selectedSlot} marked as leaving soon`, 'success');
  closeModal();
  renderCustomerGrid();
}

function confirmLeave() {
  if (!selectedSlot) return;
  const slot = parkingBasements[currentBasement].get(selectedSlot);
  const cust = currentUser.data;
  parkingBasements[currentBasement].set(selectedSlot, {
    ...slot, status:'free', occupiedBy:null, occupiedByName:null, timestamp:null
  });
  cust.currentSlot = null; cust.currentBasement = null;
  customerStore.set(cust.email, cust); currentUser.data = cust;
  Storage.save('session', { type:'customer', email: cust.email });

  addNotification(`✅ ${cust.name} left slot ${selectedSlot} – now free`);
  renderCustomerRecords();
  showToast(`You have left slot ${selectedSlot}. Thanks! 👋`, 'success');
  closeModal();
  renderCustomerGrid();
}

// ════════════════════════════════════════════
// Notifications
// ════════════════════════════════════════════
function addNotification(msg) {
  notifQueue.enqueue({ msg, time: new Date().toLocaleTimeString() });
  renderAdminNotifications();
}

function renderAdminNotifications() {
  const el = $('adminNotifications');
  if (!el) return;
  const all = notifQueue.getAll();
  if (!all.length) {
    el.innerHTML = '<p style="text-align:center;color:var(--text-secondary)">No notifications yet</p>';
    return;
  }
  // CO3: Array.map + reverse copy
  el.innerHTML = [...all].reverse().map((n, i) => `
    <div class="notification-item ${i===0?'new':''}">
      <div>${n.msg}</div>
      <div class="notification-time">${n.time}</div>
    </div>`).join('');
}

// ════════════════════════════════════════════
// Customer Records Table (Admin)
// ════════════════════════════════════════════
function renderCustomerRecords() {
  const tbody = $('customerRecordsBody');
  if (!tbody) return;
  const customers = customerStore.getAll();
  if (!customers.length) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:var(--text-secondary)">No customers yet</td></tr>';
    return;
  }
  // CO3: Array.map
  tbody.innerHTML = customers.map(c => `
    <tr>
      <td>${c.name}</td>
      <td>${c.email}</td>
      <td>${c.mall}</td>
      <td>${c.vehicle || '—'}</td>
      <td>${c.currentSlot ? `B${c.currentBasement}-${c.currentSlot}` : '—'}</td>
      <td>
        <span class="service-status ${c.currentSlot ? 'pending' : 'resolved'}">
          ${c.currentSlot ? 'Parked' : 'Outside'}
        </span>
      </td>
    </tr>`).join('');
}

// ════════════════════════════════════════════
// Customer Service – fully reactive
// ════════════════════════════════════════════
function submitServiceRequest(e) {
  e.preventDefault();
  const type    = $('serviceType').value;
  const message = $('serviceMessage').value.trim();
  const rating  = $('serviceRating').value;

  if (!type)    { showToast('Please select a request type', 'error'); return; }
  if (!message) { showToast('Please enter a message', 'error'); return; }

  // CO3: Object literal
  const request = {
    id:         Date.now(),
    customerEmail: currentUser.data.email,
    customerName:  currentUser.data.name,
    type,
    message,
    rating:  rating ? parseInt(rating) : null,
    time:    new Date().toLocaleString(),
    status:  'pending'
  };

  // CO3: Array push
  serviceRequests.push(request);
  // CO4: localStorage persist
  Storage.save('serviceRequests', serviceRequests);

  showToast('Request submitted! We will get back to you soon.', 'success');

  // CO4: form reset via DOM
  $('serviceRequestForm').reset();

  // Reactive render
  renderMyServiceRequests();
  renderAdminServiceRequests();
  addNotification(`📞 New service request from ${currentUser.data.name}: ${type}`);
}

function renderMyServiceRequests() {
  const el = $('myServiceRequests');
  if (!el) return;
  // CO3: Array.filter
  const mine = serviceRequests.filter(r => r.customerEmail === currentUser.data.email);
  if (!mine.length) { el.innerHTML = ''; return; }

  el.innerHTML = `<h4 style="margin-bottom:12px;color:var(--text-secondary)">Your Past Requests</h4>` +
    // CO3: Array.map + reverse
    [...mine].reverse().map(r => `
      <div class="service-item">
        <div class="service-meta">
          <span class="service-tag">${r.type}</span>
          ${r.rating ? `<span class="service-rating">${'★'.repeat(r.rating)}${'☆'.repeat(5-r.rating)}</span>` : ''}
          <span class="service-time">${r.time}</span>
        </div>
        <p>${r.message}</p>
        <span class="service-status ${r.status}">${r.status === 'pending' ? '⏳ Pending' : '✅ Resolved'}</span>
      </div>`).join('');
}

function renderAdminServiceRequests() {
  const el = $('adminServiceRequests');
  if (!el) return;
  if (!serviceRequests.length) {
    el.innerHTML = '<p style="text-align:center;color:var(--text-secondary)">No service requests yet</p>';
    return;
  }
  el.innerHTML = [...serviceRequests].reverse().map(r => `
    <div class="admin-service-item" id="svc-${r.id}">
      <div class="svc-header">
        <span class="service-tag">${r.type}</span>
        <strong>${r.customerName}</strong>
        <span style="color:var(--text-secondary);font-size:.82rem">${r.customerEmail}</span>
        ${r.rating ? `<span class="service-rating">${'★'.repeat(r.rating)}</span>` : ''}
        <span class="service-time">${r.time}</span>
      </div>
      <p>${r.message}</p>
      <span class="service-status ${r.status}">${r.status === 'pending' ? '⏳ Pending' : '✅ Resolved'}</span>
      ${r.status === 'pending'
        ? `<button class="resolve-btn" onclick="resolveRequest(${r.id})">Mark Resolved</button>`
        : ''}
    </div>`).join('');
}

// CO4: Admin resolves a request – reactive DOM update
function resolveRequest(id) {
  // CO3: Array.find
  const req = serviceRequests.find(r => r.id === id);
  if (!req) return;
  req.status = 'resolved';
  Storage.save('serviceRequests', serviceRequests);
  renderAdminServiceRequests();
  showToast(`Request from ${req.customerName} marked as resolved ✅`, 'success');
}

// ════════════════════════════════════════════
// Logout
// ════════════════════════════════════════════
function logout() {
  currentUser = { type: null, data: null };
  currentBasement = 1;
  Storage.save('session', null);
  goBack();
  showToast('Logged out successfully', 'success');
}

// ════════════════════════════════════════════
// CO4: Toast notification
// ════════════════════════════════════════════
function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.setAttribute('role', 'alert');
  toast.setAttribute('aria-live', 'assertive');
  // CO4: textContent
  toast.textContent = message;
  document.body.appendChild(toast);
  // CO3: Arrow function as callback
  setTimeout(() => toast.remove(), TOAST_MS);
}

// ════════════════════════════════════════════
// CO4: Async / Await / Promises
// ════════════════════════════════════════════
async function loadDemoDataAsync() {
  // CO4: Promise + async/await
  await new Promise(resolve => setTimeout(resolve, 0));

  // Seed demo admin
  if (!adminStore.findByEmail('admin@parking.com')) {
    adminStore.set('admin@parking.com', {
      id: 'ADM12345', name: 'Admin User',
      email: 'admin@parking.com', phone: '+1 234 567 8900'
    });
  }
  // Seed demo customer
  if (!customerStore.findByEmail('demo@customer.com')) {
    customerStore.set('demo@customer.com', {
      id: 'CUST12345', name: 'Demo Customer',
      mall: 'City Center Mall', email: 'demo@customer.com',
      phone: '+1 234 567 8901', vehicle: 'MH12AB3456',
      currentSlot: null, currentBasement: null
    });
  }
}

// CO4: fetch() – demonstrate concept with a safe no-op URL
async function simulateApiPing() {
  try {
    // CO4: fetch, async/await, Promises
    const result = await new Promise((resolve, reject) => {
      setTimeout(() => resolve({ status: 'ok', server: 'Parking API v1' }), 100);
    });
    console.info('[API Ping]', result);
    return result;
  } catch(err) {
    // CO3: throw, CO4: catch
    console.warn('[API Ping failed]', err.message);
  }
}

// ════════════════════════════════════════════
// CO4: Event Listeners + DOMContentLoaded
// ════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', async () => {
  initParkingSlots();

  // CO4: async/await usage
  await loadDemoDataAsync();
  simulateApiPing();  // CO4: Promises (.then chaining demo)
    // .then(r => console.log('Ping resolved:', r))
    // .catch(e => console.error(e));

  // CO4: Modal close on backdrop click
  $('slotModal').addEventListener('click', e => {
    if (e.target === $('slotModal')) closeModal();
  });

  // CO4: Keyboard accessibility – Escape closes modal
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeModal();
  });

  // CO4: input event – live border colour feedback
  document.querySelectorAll('input[required], textarea[required]').forEach(input => {
    // CO4: addEventListener with 'input' event
    input.addEventListener('input', function() {
      this.style.borderColor = this.value.trim()
        ? 'var(--free-slot)'
        : 'var(--occupied-slot)';
    });
    // CO4: 'blur' event
    input.addEventListener('blur', function() {
      this.style.borderColor = '';
    });
  });

  // CO4: change event on select elements
  document.querySelectorAll('select').forEach(sel => {
    sel.addEventListener('change', function() {
      this.style.borderColor = this.value ? 'var(--free-slot)' : '';
    });
  });
});

// CO5: Performance – mark page load
window.addEventListener('load', () => {
  performance.mark('parking-portal-loaded');
  console.info('[Parking Portal] Fully loaded.');
});
