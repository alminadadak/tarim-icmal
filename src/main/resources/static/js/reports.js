document.addEventListener('DOMContentLoaded', () => {


    const btnIcmal = document.getElementById('tab-btn-icmal');
    const btnRaporlar = document.getElementById('tab-btn-raporlar');
    const btnYonetim = document.getElementById('tab-btn-yonetim'); // YENİ
    
    const sekmeIcmal = document.getElementById('sekme-icmal');
    const sekmeRaporlar = document.getElementById('sekme-raporlar');
    const sekmeYonetim = document.getElementById('sekme-yonetim'); // YENİ

    const activeClass = ['text-agri-green', 'border-agri-green', 'bg-agri-light/30'];
    const inactiveClass = ['text-gray-500', 'border-transparent', 'hover:text-gray-700', 'hover:border-gray-300'];

    function changeTab(activeBtn, activeSekme) {
        [sekmeIcmal, sekmeRaporlar, sekmeYonetim].forEach(s => s.classList.add('hidden'));
        [btnIcmal, btnRaporlar, btnYonetim].forEach(b => {
            b.classList.remove(...activeClass);
            b.classList.add(...inactiveClass);
        });

        activeSekme.classList.remove('hidden');
        activeBtn.classList.remove(...inactiveClass);
        activeBtn.classList.add(...activeClass);
    }

    btnIcmal.addEventListener('click', () => changeTab(btnIcmal, sekmeIcmal));
    btnYonetim.addEventListener('click', () => changeTab(btnYonetim, sekmeYonetim));
    btnRaporlar.addEventListener('click', () => {
        changeTab(btnRaporlar, sekmeRaporlar);
        if (typeof currentIlAdi !== 'undefined' && currentIlAdi) {
            loadReportsTab(currentIlAdi, currentSezon);
        }
    });


    const modalHTML = `
    <div id="rapor-modal-backdrop" class="hidden fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div class="bg-white rounded-2xl shadow-xl p-6 w-full max-w-md mx-4">
            <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-bold text-gray-800"><i class="fa-solid fa-file-pen text-indigo-600 mr-2"></i>Yeni Saha Notu</h3>
                <button id="rapor-modal-kapat" class="text-gray-400 hover:text-red-500 transition-colors"><i class="fa-solid fa-xmark text-xl"></i></button>
            </div>
            <form id="rapor-form" class="space-y-4">
                <div>
                    <label class="block text-xs font-semibold text-gray-500 mb-1">İl (Otomatik)</label>
                    <input type="text" id="rapor-il" class="w-full bg-gray-100 border border-gray-200 text-gray-700 text-sm font-semibold rounded-lg p-2.5 outline-none cursor-not-allowed" readonly>
                </div>
                <div class="flex gap-3">
                    <div class="flex-1">
                        <label class="block text-xs font-semibold text-gray-500 mb-1">Kategori</label>
                        <select id="rapor-kategori" class="w-full bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 outline-none cursor-pointer" required>
                            <option value="" disabled selected>Seçiniz</option>
                            <option value="Görüşme">Görüşme</option>
                            <option value="Değerlendirme">Değerlendirme</option>
                            <option value="Sorun">Sorun</option>
                            <option value="Toplantı">Toplantı</option>
                        </select>
                    </div>
                    <div class="flex-1">
                        <label class="block text-xs font-semibold text-gray-500 mb-1">Tarih</label>
                        <input type="date" id="rapor-tarih" class="w-full bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 outline-none cursor-pointer" required>
                    </div>
                </div>
                <div>
                    <label class="block text-xs font-semibold text-gray-500 mb-1">Notlar / Rapor İçeriği</label>
                    <textarea id="rapor-notlar" rows="4" class="w-full bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-indigo-500 focus:border-indigo-500 p-2.5 outline-none" placeholder="Görüşme detaylarını veya sorunları buraya yazın..." required></textarea>
                </div>
                <button type="submit" class="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-lg text-sm px-5 py-3 transition-colors shadow-md mt-2">Kaydet</button>
            </form>
        </div>
    </div>`;
    document.body.insertAdjacentHTML('beforeend', modalHTML);

    const raporModal = document.getElementById('rapor-modal-backdrop');
    const raporForm = document.getElementById('rapor-form');
    
    document.getElementById('btn-yeni-rapor').addEventListener('click', () => {
        if (!currentIlAdi) {
            if (typeof showToast === 'function') showToast('Lütfen önce bir il seçin.', 'warning');
            return;
        }
        document.getElementById('rapor-il').value = currentIlAdi;
        // Tarihi bugüne ayarla
        document.getElementById('rapor-tarih').value = new Date().toISOString().split('T')[0];
        raporModal.classList.remove('hidden');
    });

    document.getElementById('rapor-modal-kapat').addEventListener('click', () => {
        raporModal.classList.add('hidden');
        raporForm.reset();
    });


    raporForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitBtn = raporForm.querySelector('button[type="submit"]');
        const orjinalIcerik = submitBtn.innerHTML;
        
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i> Buluta İşleniyor...';
        submitBtn.classList.add('opacity-75', 'cursor-not-allowed');

        const payload = {
            il: document.getElementById('rapor-il').value,
            kategori: document.getElementById('rapor-kategori').value,
            raporTarihi: document.getElementById('rapor-tarih').value,
            notlar: document.getElementById('rapor-notlar').value
        };

        try {
            const response = await fetch('/api/reports', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) throw new Error('Sunucu hatası');
            if (typeof showToast === 'function') showToast('Rapor başarıyla E-Tabloya eklendi!', 'success');
            
            raporModal.classList.add('hidden');
            raporForm.reset();
            loadPastReports(currentIlAdi); 
        } catch (error) {
            console.error(error);
            if (typeof showToast === 'function') showToast('Rapor kaydedilemedi!', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = orjinalIcerik;
            submitBtn.classList.remove('opacity-75', 'cursor-not-allowed');
        }
    });

    window.loadReportsTab = async function(ilAdi, sezon) {
        document.getElementById('ai-summary-content').innerHTML = '<span class="text-gray-500 italic text-sm"><i class="fa-solid fa-spinner fa-spin mr-2"></i>Veriler analiz ediliyor...</span>';
        
        loadPastReports(ilAdi);
        generateSmartSummary(ilAdi, sezon);
    };

    async function generateSmartSummary(ilAdi, sezon) {
        try {
            const [kislisRes, yazlikRes, uyduRes] = await Promise.all([
                fetch(`/api/icmal?il=${encodeURIComponent(ilAdi)}&donem=Kışlık&sezon=${sezon}`),
                fetch(`/api/icmal?il=${encodeURIComponent(ilAdi)}&donem=Yazlık&sezon=${sezon}`),
                fetch(`/api/reports/classifications/summary?il=${encodeURIComponent(ilAdi)}&sezon=${sezon}`) // YENİ
            ]);

            let kislisData = kislisRes.ok ? await kislisRes.json() : [];
            let yazlikData = yazlikRes.ok ? await yazlikRes.json() : [];
            let uyduData = uyduRes.ok ? await uyduRes.json() : { EGITIM_SAYISI: 0, TAHMIN_SAYISI: 0 };

            const analizEt = (data) => {
                return data
                    .filter(r => r.tamamlananYuzde !== null && r.tamamlananYuzde < 70 && r.nihaiAlan <= 10000)
                    .map(r => r.urun);
            };

            const kislisYetersizler = analizEt(kislisData);
            const yazlikYetersizler = analizEt(yazlikData);

            let kislisMetni = "";
            if (kislisData.length > 0) {
                if (kislisYetersizler.length > 0) {
                    kislisMetni = `kışlık ürünlerde nihai referans dataları sisteme girilmiş olup, sisteme <b>${kislisYetersizler.join(', ')}</b> girişinin yetersiz olduğu görülmektedir.`;
                } else {
                    kislisMetni = `kışlık ürünlerde nihai referans datalarının sisteme yeterli seviyede girildiği tespit edilmiştir.`;
                }
            } else {
                kislisMetni = `kışlık ürünlere ait sisteme girilmiş herhangi bir veri bulunmamaktadır.`;
            }

            let yazlikMetni = "";
            if (yazlikData.length > 0) {
                if (yazlikYetersizler.length > 0) {
                    yazlikMetni = `Buna ek olarak yazlık ürünlerde de nihai referans dataları sisteme girilmiş olup; sisteme <b>${yazlikYetersizler.join(', ')}</b> girişinin yetersiz olduğu tespit edilmiştir.`;
                } else {
                    yazlikMetni = `Buna ek olarak yazlık ürün veri girişlerinin beklenen düzeyde tamamlandığı tespit edilmiştir.`;
                }
            } else {
                yazlikMetni = `Yazlık ürünlere ait veri girişi ise henüz sağlanmamıştır.`;
            }

            let uyduMetni = `Ayrıca Uydu Takip Sistemi üzerinden ${sezon} sezonunda bu il için toplam <strong>${uyduData.EGITIM_SAYISI}</strong> eğitim ve <strong>${uyduData.TAHMIN_SAYISI}</strong> tahmin işlemi başarıyla tamamlanmıştır.`;

            const tamMetin = `
                <p class="text-justify text-gray-800 leading-relaxed mb-3" style="text-indent: 2rem;">
                    Yapılan incelemeler neticesinde; <strong>${ilAdi}</strong> ili <strong>${sezon}</strong> üretim sezonuna ait icmal verileri değerlendirildiğinde, ${kislisMetni} ${yazlikMetni}
                </p>
                <p class="text-justify text-gray-800 leading-relaxed" style="text-indent: 2rem;">
                    İlgili eksikliklerin giderilmesi ve veri kalitesinin artırılması amacıyla saha çalışmalarının yakından takip edilmesi hususu bilgilerinize sunulur. ${uyduMetni}
                </p>
            `;

            document.getElementById('ai-summary-content').innerHTML = tamMetin;

        } catch (error) {
            console.error("Özet oluşturulurken hata:", error);
            document.getElementById('ai-summary-content').innerHTML = '<span class="text-red-500 text-sm">Analiz raporu oluşturulamadı.</span>';
        }
    }

    async function loadPastReports(ilAdi) {
        const listDiv = document.getElementById('reports-list');
        listDiv.innerHTML = '<span class="text-gray-400 text-sm italic">Kayıtlar aranıyor...</span>';

        try {
            const res = await fetch(`/api/reports?il=${encodeURIComponent(ilAdi)}`);
            if (!res.ok) throw new Error('Veri çekilemedi');
            const reports = await res.json();

            const sayac = {
                'Görüşme': 0,
                'Değerlendirme': 0,
                'Sorun': 0,
                'Toplantı': 0
            };
            reports.forEach(r => {
                if(sayac[r.kategori] !== undefined) sayac[r.kategori]++;
            });


            const statsHtml = `
                <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
                    <!-- Görüşme Kartı -->
                    <div class="bg-green-50 border border-green-100 rounded-xl p-3 flex items-center justify-between shadow-sm hover:shadow-md transition-shadow">
                        <div>
                            <p class="text-[10px] text-green-600 font-bold uppercase tracking-wider mb-0.5">Görüşme</p>
                            <p class="text-xl font-black text-green-800 leading-none">${sayac['Görüşme']}</p>
                        </div>
                        <div class="bg-green-200 text-green-700 w-8 h-8 flex items-center justify-center rounded-lg shadow-sm">
                            <i class="fa-solid fa-handshake text-sm"></i>
                        </div>
                    </div>
                    <!-- Değerlendirme Kartı -->
                    <div class="bg-blue-50 border border-blue-100 rounded-xl p-3 flex items-center justify-between shadow-sm hover:shadow-md transition-shadow">
                        <div>
                            <p class="text-[10px] text-blue-600 font-bold uppercase tracking-wider mb-0.5">Değerlendirme</p>
                            <p class="text-xl font-black text-blue-800 leading-none">${sayac['Değerlendirme']}</p>
                        </div>
                        <div class="bg-blue-200 text-blue-700 w-8 h-8 flex items-center justify-center rounded-lg shadow-sm">
                            <i class="fa-solid fa-chart-line text-sm"></i>
                        </div>
                    </div>
                    <!-- Sorun Kartı -->
                    <div class="bg-red-50 border border-red-100 rounded-xl p-3 flex items-center justify-between shadow-sm hover:shadow-md transition-shadow">
                        <div>
                            <p class="text-[10px] text-red-600 font-bold uppercase tracking-wider mb-0.5">Sorun</p>
                            <p class="text-xl font-black text-red-800 leading-none">${sayac['Sorun']}</p>
                        </div>
                        <div class="bg-red-200 text-red-700 w-8 h-8 flex items-center justify-center rounded-lg shadow-sm">
                            <i class="fa-solid fa-triangle-exclamation text-sm"></i>
                        </div>
                    </div>
                    <!-- Toplantı Kartı -->
                    <div class="bg-purple-50 border border-purple-100 rounded-xl p-3 flex items-center justify-between shadow-sm hover:shadow-md transition-shadow">
                        <div>
                            <p class="text-[10px] text-purple-600 font-bold uppercase tracking-wider mb-0.5">Toplantı</p>
                            <p class="text-xl font-black text-purple-800 leading-none">${sayac['Toplantı']}</p>
                        </div>
                        <div class="bg-purple-200 text-purple-700 w-8 h-8 flex items-center justify-center rounded-lg shadow-sm">
                            <i class="fa-solid fa-users text-sm"></i>
                        </div>
                    </div>
                </div>
            `;

            if (reports.length === 0) {
                listDiv.innerHTML = statsHtml + '<div class="p-4 bg-gray-50 border border-dashed border-gray-300 rounded-lg text-center text-sm text-gray-500">Bu ile ait henüz bir saha notu girilmemiş.</div>';
                return;
            }

            listDiv.innerHTML = statsHtml;

            reports.forEach(r => {
                const d = r.raporTarihi.split('-');
                const trTarih = `${d[2]}.${d[1]}.${d[0]}`;

                let badge = 'bg-gray-100 text-gray-800';
                if (r.kategori === 'Sorun') badge = 'bg-red-100 text-red-800 border-red-200';
                else if (r.kategori === 'Değerlendirme') badge = 'bg-blue-100 text-blue-800 border-blue-200';
                else if (r.kategori === 'Görüşme') badge = 'bg-green-100 text-green-800 border-green-200';
                else if (r.kategori === 'Toplantı') badge = 'bg-purple-100 text-purple-800 border-purple-200';

                const html = `
                <div class="bg-white p-4 rounded-xl border border-gray-100 shadow-sm relative pl-4 border-l-4 ${r.kategori === 'Sorun' ? 'border-l-red-500' : 'border-l-indigo-400'} hover:shadow-md transition-shadow">
                    <div class="flex justify-between items-start mb-2">
                        <span class="text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-wider ${badge}">${r.kategori}</span>
                        <span class="text-xs text-gray-400 font-semibold flex items-center gap-1"><i class="fa-regular fa-calendar"></i> ${trTarih}</span>
                    </div>
                    <p class="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">${r.notlar}</p>
                </div>`;
                listDiv.insertAdjacentHTML('beforeend', html);
            });

        } catch (error) {
            listDiv.innerHTML = '<span class="text-red-500 text-sm">Raporlar yüklenemedi.</span>';
        }
    }

    document.getElementById('city-select').addEventListener('change', () => {
        if (!sekmeRaporlar.classList.contains('hidden')) {
            const seciliIl = document.getElementById('city-select').value;
            const seciliSezon = document.getElementById('season-select').value;
            if (seciliIl) loadReportsTab(seciliIl, seciliSezon);
        }
    });

    document.getElementById('season-select').addEventListener('change', () => {
        if (!sekmeRaporlar.classList.contains('hidden')) {
            const seciliIl = document.getElementById('city-select').value;
            const seciliSezon = document.getElementById('season-select').value;
            if (seciliIl) loadReportsTab(seciliIl, seciliSezon);
        }
    });

});