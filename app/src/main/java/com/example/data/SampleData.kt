package com.example.data

import com.example.model.AssetCondition
import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.model.VehicleType
import java.util.Calendar

object SampleData {

    fun getInitialAssets(): List<AssetEntity> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Helper to get time shifted by months/days
        fun shiftTime(yearDiff: Int = 0, monthDiff: Int = 0, dayDiff: Int = 0): Long {
            return Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.YEAR, yearDiff)
                add(Calendar.MONTH, monthDiff)
                add(Calendar.DAY_OF_YEAR, dayDiff)
            }.timeInMillis
        }

        return listOf(
            // --- KENDARAAN (Mobil & Motor) ---
            AssetEntity(
                name = "Toyota Kijang Innova Zenix 2.0 Q HV",
                code = "AST-KND-001",
                type = AssetType.KENDARAAN,
                acquisitionCost = 625_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -1, monthDiff = -4), // ~16 bulan lalu
                usefulLifeYears = 8,
                salvageValue = 120_000_000.0,
                location = "Pool Kantor Pusat Jakarta",
                pic = "Bambang Sudiro (Direktur Operasional)",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.SANGAT_BAIK,
                notes = "Fasilitas dinas Direksi. Servis rutin berkala di bengkel resmi Auto2000.",
                vehicleType = VehicleType.MOBIL,
                licensePlate = "B 1024 SPO",
                engineNumber = "M20A-FXS-981244",
                chassisNumber = "MHFA31B88P001923",
                bpkbNumber = "M-09823412-JKT",
                stnkNumber = "STNK-09182390",
                annualTaxDueDate = shiftTime(dayDiff = 12), // Kritis: 12 hari lagi
                fiveYearPlateDueDate = shiftTime(yearDiff = 3, monthDiff = 8),
                annualTaxAmount = 9_850_000.0,
                lastServiceDate = shiftTime(monthDiff = -1, dayDiff = -10)
            ),
            AssetEntity(
                name = "Honda Vario 160 CBS",
                code = "AST-KND-002",
                type = AssetType.KENDARAAN,
                acquisitionCost = 28_500_000.0,
                acquisitionDate = shiftTime(yearDiff = -1, monthDiff = -1),
                usefulLifeYears = 5,
                salvageValue = 5_000_000.0,
                location = "Parkir Kurir Kantor Pusat",
                pic = "Ahmad Fadhil (Divisi Logistik & Kurir)",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.BAIK,
                notes = "Kendaraan operasional ekspedisi dokumen harian & perbankan.",
                vehicleType = VehicleType.MOTOR,
                licensePlate = "B 4521 TKL",
                engineNumber = "KF12E-1829104",
                chassisNumber = "MH1KF1219PK01284",
                bpkbNumber = "L-48192034-JKT",
                stnkNumber = "STNK-11827402",
                annualTaxDueDate = shiftTime(dayDiff = 25), // Warning: 25 hari lagi
                fiveYearPlateDueDate = shiftTime(yearDiff = 2, monthDiff = 11),
                annualTaxAmount = 450_000.0,
                lastServiceDate = shiftTime(monthDiff = -2)
            ),
            AssetEntity(
                name = "Toyota Hilux 2.4 D-Cab 4x4",
                code = "AST-KND-003",
                type = AssetType.KENDARAAN,
                acquisitionCost = 490_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -2, monthDiff = -3),
                usefulLifeYears = 8,
                salvageValue = 90_000_000.0,
                location = "Site Proyek Konstruksi Cilegon",
                pic = "Danang Prasetyo (Project Manager)",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.PERLU_PERBAIKAN,
                notes = "Pajak tahunan telah jatuh tempo 5 hari lalu. Butuh perpanjangan segera.",
                vehicleType = VehicleType.MOBIL,
                licensePlate = "D 9012 AB",
                engineNumber = "2GD-FTV-881023",
                chassisNumber = "MR0HA3CD6K019280",
                bpkbNumber = "N-77123991-BDG",
                stnkNumber = "STNK-55291801",
                annualTaxDueDate = shiftTime(dayDiff = -5), // EXPIRED: lewat 5 hari!
                fiveYearPlateDueDate = shiftTime(yearDiff = 1, monthDiff = 2),
                annualTaxAmount = 7_200_000.0,
                lastServiceDate = shiftTime(monthDiff = -3)
            ),
            AssetEntity(
                name = "Mitsubishi Pajero Sport 2.4 Dakar 4x2",
                code = "AST-KND-004",
                type = AssetType.KENDARAAN,
                acquisitionCost = 710_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -1, monthDiff = -8),
                usefulLifeYears = 8,
                salvageValue = 150_000_000.0,
                location = "Pool Kantor Pusat Jakarta",
                pic = "Suryo Utomo (Direktur Utama)",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.SANGAT_BAIK,
                notes = "Mobil dinas Dirut. Kondisi mesin prima, asuransi all-risk aktif.",
                vehicleType = VehicleType.MOBIL,
                licensePlate = "B 8899 RFS",
                engineNumber = "4N15-U092812",
                chassisNumber = "MMBNP1W4NPH01299",
                bpkbNumber = "K-88123912-JKT",
                stnkNumber = "STNK-99281034",
                annualTaxDueDate = shiftTime(monthDiff = 5, dayDiff = 10), // Aman (>160 hari)
                fiveYearPlateDueDate = shiftTime(yearDiff = 4, monthDiff = 2),
                annualTaxAmount = 11_400_000.0,
                lastServiceDate = shiftTime(monthDiff = -1)
            ),
            AssetEntity(
                name = "Yamaha NMAX 155 Connected ABS",
                code = "AST-KND-005",
                type = AssetType.KENDARAAN,
                acquisitionCost = 35_500_000.0,
                acquisitionDate = shiftTime(yearDiff = -1),
                usefulLifeYears = 5,
                salvageValue = 7_000_000.0,
                location = "Cabang Bekasi",
                pic = "Wahyu Setiawan (Supervisor Lapangan)",
                status = AssetStatus.STANDBY,
                condition = AssetCondition.BAIK,
                notes = "Kendaraan cadangan operasional cabang dan patroli keamanan.",
                vehicleType = VehicleType.MOTOR,
                licensePlate = "B 3341 PRQ",
                engineNumber = "G3J4E-048192",
                chassisNumber = "MH3SG5610NK01928",
                bpkbNumber = "L-99213812-BKS",
                stnkNumber = "STNK-77182903",
                annualTaxDueDate = shiftTime(monthDiff = 3, dayDiff = 15), // Aman
                fiveYearPlateDueDate = shiftTime(yearDiff = 3, monthDiff = 4),
                annualTaxAmount = 520_000.0,
                lastServiceDate = shiftTime(monthDiff = -2)
            ),

            // --- TANAH ---
            AssetEntity(
                name = "Tanah Kawasan Industri KIIC Karawang",
                code = "AST-TNH-001",
                type = AssetType.TANAH,
                acquisitionCost = 4_200_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -4),
                usefulLifeYears = 0, // Tanah tidak disusutkan
                salvageValue = 4_200_000_000.0,
                location = "Jl. Permata Raya Lot CA-5, Kawasan Industri KIIC, Karawang Barat",
                pic = "General Affairs & Legal Corporate",
                status = AssetStatus.STANDBY,
                condition = AssetCondition.SANGAT_BAIK,
                notes = "Rencana peruntukan gudang distribusi regional Jawa Barat. Bebas sengketa.",
                certificateType = "SHM (Sertifikat Hak Milik)",
                certificateNumber = "SHM No. 04281/Telukjambe",
                pbbNop = "32.15.010.002.005-0120.0",
                landAreaM2 = 3500.0
            ),
            AssetEntity(
                name = "Lahan Gudang Logistik Marunda",
                code = "AST-TNH-002",
                type = AssetType.TANAH,
                acquisitionCost = 2_700_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -3),
                usefulLifeYears = 0,
                salvageValue = 2_700_000_000.0,
                location = "Kawasan Pergudangan Marunda Center Kav. 18, Cilincing, Jakarta Utara",
                pic = "Divisi Rantai Pasok (Supply Chain)",
                status = AssetStatus.DISEWAKAN,
                condition = AssetCondition.BAIK,
                notes = "Saat ini sebagian disewakan ke mitra logistik pihak ketiga.",
                certificateType = "HGB (Hak Guna Bangunan)",
                certificateNumber = "HGB No. 1290/Marunda",
                pbbNop = "31.72.040.001.008-0091.0",
                landAreaM2 = 1800.0
            ),

            // --- RUMAH & BANGUNAN ---
            AssetEntity(
                name = "Gedung Kantor Pusat Wisma Mandiri Lt. 12",
                code = "AST-BGN-001",
                type = AssetType.BANGUNAN,
                acquisitionCost = 6_800_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -3, monthDiff = -6),
                usefulLifeYears = 20,
                salvageValue = 1_000_000_000.0,
                location = "Jl. MH Thamrin No. 5, Gondangdia, Menteng, Jakarta Pusat",
                pic = "Building Management & IT",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.SANGAT_BAIK,
                notes = "Ruang kantor utama perusahaan termasuk boardroom eksekutif & data center.",
                certificateType = "Strata Title (Sertifikat Hak Milik Satuan Rumah Susun)",
                certificateNumber = "SHMSRS No. 892/Menteng",
                pbgNumber = "PBG-3171-2021-0089",
                buildingAreaM2 = 580.0,
                numberOfFloors = 1
            ),
            AssetEntity(
                name = "Ruko Kantor Cabang BSD City 3 Lantai",
                code = "AST-BGN-002",
                type = AssetType.BANGUNAN,
                acquisitionCost = 3_400_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -2, monthDiff = -2),
                usefulLifeYears = 20,
                salvageValue = 500_000_000.0,
                location = "Ruko Golden Boulevard Blok W2 No. 15, Serpong, Tangerang Selatan",
                pic = "Kepala Cabang Tangerang",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.BAIK,
                notes = "Kantor cabang operasional wilayah Banten & customer service lounge.",
                certificateType = "HGB Murni",
                certificateNumber = "HGB No. 2049/Serpong",
                pbgNumber = "IMB-640-TNG-2020",
                landAreaM2 = 120.0,
                buildingAreaM2 = 320.0,
                numberOfFloors = 3
            ),

            // --- INVENTARIS KANTOR ---
            AssetEntity(
                name = "Server Enterprise Dell PowerEdge R750",
                code = "AST-INV-001",
                type = AssetType.INVENTARIS,
                acquisitionCost = 165_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -1, monthDiff = -6),
                usefulLifeYears = 4,
                salvageValue = 15_000_000.0,
                location = "Ruang Server Lt. 12 (Rak A-02)",
                pic = "Hendra Gunawan (Lead DevOps & Infra)",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.SANGAT_BAIK,
                notes = "Main on-premise virtualization host & primary database server cluster.",
                brandModel = "Dell EMC PowerEdge R750 2x Xeon Gold 6330 256GB RAM",
                serialNumber = "SN-DELL-R750-X991823",
                department = "Divisi IT & Infrastruktur"
            ),
            AssetEntity(
                name = "Apple MacBook Pro 16 M3 Max 36GB",
                code = "AST-INV-002",
                type = AssetType.INVENTARIS,
                acquisitionCost = 48_500_000.0,
                acquisitionDate = shiftTime(monthDiff = -8),
                usefulLifeYears = 4,
                salvageValue = 8_000_000.0,
                location = "Studio Desain Lt. 12",
                pic = "Rizky Ramadhan (Senior Product Designer)",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.SANGAT_BAIK,
                notes = "Perangkat kerja UI/UX designer & visual rendering.",
                brandModel = "MacBook Pro 16 Space Black M3 Max 1TB SSD",
                serialNumber = "C02G901KMD6T",
                department = "Product & Design"
            ),
            AssetEntity(
                name = "Smart Interactive Display Samsung Flip Pro 75\"",
                code = "AST-INV-003",
                type = AssetType.INVENTARIS,
                acquisitionCost = 55_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -1, monthDiff = -2),
                usefulLifeYears = 5,
                salvageValue = 5_000_000.0,
                location = "Boardroom Meeting Room Lt. 12",
                pic = "Sekretariat Direksi",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.BAIK,
                notes = "Layar sentuh presentasi & video conference Zoom Room bersertifikasi.",
                brandModel = "Samsung Flip Pro WM75B UHD Touch",
                serialNumber = "WM75B-00291048",
                department = "Corporate Secretary"
            ),
            AssetEntity(
                name = "Sofa Eksekutif Leather & Coffee Table Set",
                code = "AST-INV-004",
                type = AssetType.INVENTARIS,
                acquisitionCost = 28_000_000.0,
                acquisitionDate = shiftTime(yearDiff = -2),
                usefulLifeYears = 5,
                salvageValue = 2_000_000.0,
                location = "Lobby Tamu VIP Lt. 12",
                pic = "Resepsionis & GA",
                status = AssetStatus.DIGUNAKAN,
                condition = AssetCondition.BAIK,
                notes = "Sofa kulit sintetis premium untuk ruang tunggu tamu direksi.",
                brandModel = "Informa Executive Lounge Set",
                serialNumber = "INV-FURN-2022-044",
                department = "General Affairs"
            )
        )
    }
}
