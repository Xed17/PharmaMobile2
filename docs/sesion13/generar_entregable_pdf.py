import os, sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_header_footer(num_pages)
            super().showPage()
        super().save()

    def draw_header_footer(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        
        # Don't draw header/footer on cover page
        if self._pageNumber > 1:
            # Header
            self.drawString(54, 750, "Universidad Peruana Unión · Desarrollo de Aplicaciones Móviles")
            self.drawRightString(612 - 54, 750, "Sesión 13: Proyecto Integrador")
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.5)
            self.line(54, 742, 612 - 54, 742)
            
            # Footer
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.5)
            self.line(54, 45, 612 - 54, 45)
            self.drawString(54, 32, "PharmaMobil (KMP) · Rama feature/integrador-13")
            page_text = f"Página {self._pageNumber} de {page_count}"
            self.drawRightString(612 - 54, 32, page_text)
            
        self.restoreState()

def build_pdf():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    evidencias_dir = os.path.join(base_dir, "evidencias")
    pdf_path = os.path.join(base_dir, "Entregable_Sesion13_PharmaMobil.pdf")
    
    doc = SimpleDocTemplate(
        pdf_path,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )
    
    styles = getSampleStyleSheet()
    
    # Custom Palette
    c_primary = colors.HexColor("#0B2F64")     # UPeU Navy Blue
    c_secondary = colors.HexColor("#0284C7")   # Deep Cyan / Teal
    c_dark = colors.HexColor("#0F172A")        # Slate 900
    c_body = colors.HexColor("#334155")        # Slate 700
    c_light_bg = colors.HexColor("#F8FAFC")    # Slate 50
    c_border = colors.HexColor("#E2E8F0")      # Slate 200
    c_success = colors.HexColor("#166534")     # Green 800
    c_success_bg = colors.HexColor("#DCFCE7")  # Green 100
    
    # Custom Styles
    style_cover_title = ParagraphStyle(
        'CoverTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=30,
        textColor=c_primary,
        alignment=1 # Center
    )
    
    style_cover_subtitle = ParagraphStyle(
        'CoverSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=13,
        leading=18,
        textColor=c_secondary,
        alignment=1
    )
    
    style_h1 = ParagraphStyle(
        'Header1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=15,
        leading=19,
        textColor=c_primary,
        spaceBefore=14,
        spaceAfter=8,
        keepWithNext=True
    )
    
    style_h2 = ParagraphStyle(
        'Header2',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11.5,
        leading=15,
        textColor=c_secondary,
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )
    
    style_body = ParagraphStyle(
        'Body',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=c_body,
        spaceAfter=6
    )
    
    style_body_bold = ParagraphStyle(
        'BodyBold',
        parent=style_body,
        fontName='Helvetica-Bold'
    )
    
    style_table_header = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=10,
        textColor=colors.white,
        alignment=1
    )
    
    style_table_cell = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.5,
        leading=10,
        textColor=c_dark
    )
    
    style_table_cell_center = ParagraphStyle(
        'TableCellCenter',
        parent=style_table_cell,
        alignment=1
    )
    
    style_table_cell_bold = ParagraphStyle(
        'TableCellBold',
        parent=style_table_cell,
        fontName='Helvetica-Bold'
    )
    
    style_code = ParagraphStyle(
        'CodeSnippet',
        parent=styles['Normal'],
        fontName='Courier',
        fontSize=7.5,
        leading=10.5,
        textColor=colors.HexColor("#1E293B")
    )
    
    style_caption = ParagraphStyle(
        'Caption',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=11,
        textColor=c_primary,
        alignment=1,
        spaceBefore=4
    )

    story = []
    
    # -------------------------------------------------------------------------
    # PORTADA
    # -------------------------------------------------------------------------
    story.append(Spacer(1, 40))
    story.append(Paragraph("UNIVERSIDAD PERUANA UNIÓN", ParagraphStyle('Univ', parent=style_cover_title, fontSize=16, leading=20, textColor=colors.HexColor("#475569"))))
    story.append(Paragraph("FACULTAD DE INGENIERÍA Y ARQUITECTURA", ParagraphStyle('Fac', parent=style_cover_subtitle, fontSize=12, leading=16, textColor=colors.HexColor("#64748B"))))
    story.append(Paragraph("Escuela Profesional de Ingeniería de Sistemas", ParagraphStyle('Esc', parent=style_cover_subtitle, fontSize=11, leading=15, textColor=colors.HexColor("#64748B"))))
    story.append(Spacer(1, 25))
    story.append(HRFlowable(width="100%", thickness=3, color=c_primary, spaceAfter=25, spaceBefore=0))
    
    story.append(Paragraph("DESARROLLO DE APLICACIONES MÓVILES", style_cover_title))
    story.append(Spacer(1, 10))
    story.append(Paragraph("INFORME DE ENTREGABLE — SESIÓN 13 (UNIDAD 3)", ParagraphStyle('Inf', parent=style_cover_title, fontSize=17, leading=22, textColor=c_secondary)))
    story.append(Spacer(1, 8))
    story.append(Paragraph("Proyecto Integrador: Refinamiento Offline-First, Firebase y Permisos Nativos", style_cover_subtitle))
    story.append(Spacer(1, 20))
    story.append(HRFlowable(width="100%", thickness=1, color=c_secondary, spaceAfter=25, spaceBefore=0))
    
    meta_table_data = [
        [Paragraph("<b>Asignatura:</b>", style_table_cell), Paragraph("Desarrollo de Aplicaciones Móviles", style_table_cell),
         Paragraph("<b>Ciclo / Semestre:</b>", style_table_cell), Paragraph("VI · Semestre 2026-2", style_table_cell)],
        [Paragraph("<b>Unidad:</b>", style_table_cell), Paragraph("Unidad 3: Proyecto de Fin de Curso", style_table_cell),
         Paragraph("<b>Sesión:</b>", style_table_cell), Paragraph("Sesión 13 (Unidad 3, Sesión 1)", style_table_cell)],
        [Paragraph("<b>Proyecto:</b>", style_table_cell), Paragraph("PharmaMobil (KMP) sobre PharmaSoft", style_table_cell),
         Paragraph("<b>Fecha de Entrega:</b>", style_table_cell), Paragraph("4 de noviembre de 2026", style_table_cell)],
        [Paragraph("<b>Docente:</b>", style_table_cell), Paragraph("Benjamin David Reyna Barreto", style_table_cell),
         Paragraph("<b>Estudiante / Autor:</b>", style_table_cell), Paragraph("Eduard Chambilla (Xed17)", style_table_cell)],
        [Paragraph("<b>Rama de Entrega:</b>", style_table_cell), Paragraph("<code>feature/integrador-13</code>", style_table_cell),
         Paragraph("<b>Repositorio:</b>", style_table_cell), Paragraph("github.com/Xed17/PharmaMobile2", style_table_cell)],
        [Paragraph("<b>Estado de Tests:</b>", style_table_cell), Paragraph("<font color='#166534'><b>116 tests pasados (0 fallos)</b></font>", style_table_cell),
         Paragraph("<b>Build Android:</b>", style_table_cell), Paragraph("<font color='#166534'><b>BUILD SUCCESSFUL</b></font>", style_table_cell)]
    ]
    meta_table = Table(meta_table_data, colWidths=[90, 160, 95, 159])
    meta_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), c_light_bg),
        ('BOX', (0,0), (-1,-1), 1, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('PADDING', (0,0), (-1,-1), 6),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
    ]))
    story.append(meta_table)
    
    story.append(Spacer(1, 35))
    story.append(Paragraph("<b>Resumen de Verificación y Auditoría Crítica:</b>", style_h2))
    story.append(Paragraph(
        "El presente documento consolida la sustentación técnica formal y las evidencias verificables correspondientes a los <b>7 entregables de la Sesión 13</b> y a los <b>14 criterios observables de la Lista de Cotejo</b> oficial de la Universidad Peruana Unión. El desarrollo fue completado y verificado en un dispositivo emulado Android Pixel 7 (API 34) conectado en tiempo real al backend local PharmaSoft sobre Spring Boot 3.3.4, garantizando sincronización bidireccional recuperable, aislamiento criptográfico multicuenta, notificaciones locales seguras y telemetría Firebase desacoplada.",
        style_body
    ))
    story.append(PageBreak())
    
    # -------------------------------------------------------------------------
    # SECCIÓN 1: TABLA DE HUECOS (PASO 1)
    # -------------------------------------------------------------------------
    story.append(Paragraph("1. Tabla de Huecos del Flujo Completo y Decisiones (Paso 1)", style_h1))
    story.append(Paragraph(
        "Conforme al <b>Paso 1</b> de la guía, se realizó una auditoría funcional integral recorriendo los 8 momentos críticos del ciclo de vida móvil, identificando callejones sin salida, inconsistencias de persistencia y riesgos transaccionales:",
        style_body
    ))
    
    huecos_data = [
        [Paragraph("ID", style_table_header), Paragraph("Momento del Recorrido", style_table_header),
         Paragraph("Hallazgo Comprobado en Código Base", style_table_header), Paragraph("Gravedad", style_table_header),
         Paragraph("Decisión Tomada / Arquitectura", style_table_header), Paragraph("Estado", style_table_header)],
        
        [Paragraph("<b>R1</b>", style_table_cell_center), Paragraph("Primera apertura, sin sesión", style_table_cell),
         Paragraph("El botón offline fabricaba identidad admin sintética sin validación previa en el servidor.", style_table_cell),
         Paragraph("<font color='#DC2626'><b>Alta</b></font>", style_table_cell_center),
         Paragraph("Se eliminó la creación sintética. La continuidad offline se restringe exclusivamente a cuentas previamente autenticadas y validadas con JWT en SQLite.", style_table_cell),
         Paragraph("<font color='#166534'><b>CERRADO</b></font>", style_table_cell_center)],
         
        [Paragraph("<b>R2</b>", style_table_cell_center), Paragraph("Login con credenciales inválidas / sin red", style_table_cell),
         Paragraph("Mensajes de error ambiguos sin diferenciar fallo de red de credencial inválida.", style_table_cell),
         Paragraph("<font color='#D97706'><b>Media</b></font>", style_table_cell_center),
         Paragraph("Mapeo tipado en <code>SesionViewModel</code> distinguiendo <code>ErrorSesion.Red</code> vs <code>CredencialesInvalidas</code> con banner contextual no invasivo.", style_table_cell),
         Paragraph("<font color='#166534'><b>CERRADO</b></font>", style_table_cell_center)],
         
        [Paragraph("<b>R3</b>", style_table_cell_center), Paragraph("Listado con base local vacía", style_table_cell),
         Paragraph("El estado <code>SinProductos</code> no ofrecía acción explícita para forzar recuperación.", style_table_cell),
         Paragraph("<font color='#D97706'><b>Media</b></font>", style_table_cell_center),
         Paragraph("Se integró botón «Recargar» y «Reintentar descarga» en la tarjeta principal y cabecera de catálogo.", style_table_cell),
         Paragraph("<font color='#166534'><b>CERRADO</b></font>", style_table_cell_center)],
         
        [Paragraph("<b>R4</b>", style_table_cell_center), Paragraph("Creación con modo avión activado", style_table_cell),
         Paragraph("El guardado bloqueaba el hilo UI esperando el timeout síncrono HTTP de red.", style_table_cell),
         Paragraph("<font color='#DC2626'><b>Alta</b></font>", style_table_cell_center),
         Paragraph("Guardado local inmediato en SQLite con ID temporal negativo y encolado asíncrono cancelable por cuenta en <code>ColaProductoLocal</code>.", style_table_cell),
         Paragraph("<font color='#166534'><b>CERRADO</b></font>", style_table_cell_center)],
         
        [Paragraph("<b>R5</b>", style_table_cell_center), Paragraph("Sincronización al recuperar señal (POST)", style_table_cell),
         Paragraph("POST sin clave de idempotencia: reintentar ante corte de conexión duplicaba registros.", style_table_cell),
         Paragraph("<font color='#DC2626'><b>Crítica</b></font>", style_table_cell_center),
         Paragraph("Política estricta: reintento automático exclusivo para GET (idempotente). Creaciones ambiguas pasan a <code>REVISION</code> para confirmación manual asistida.", style_table_cell),
         Paragraph("<font color='#166534'><b>CERRADO</b></font>", style_table_cell_center)],
         
        [Paragraph("<b>R6</b>", style_table_cell_center), Paragraph("Error de validación del servidor (422)", style_table_cell),
         Paragraph("Una fila rechazada por el backend interrumpía todo el lote restante de sincronización.", style_table_cell),
         Paragraph("<font color='#DC2626'><b>Alta</b></font>", style_table_cell_center),
         Paragraph("Procesamiento independiente por fila: el error 422 se persiste en <code>ultimoError</code>, marcando la fila en revisión sin bloquear las demás.", style_table_cell),
         Paragraph("<font color='#166534'><b>CERRADO</b></font>", style_table_cell_center)],
         
        [Paragraph("<b>R7</b>", style_table_cell_center), Paragraph("Token vencido a mitad de formulario", style_table_cell),
         Paragraph("Se perdía el texto escrito en el formulario si la sesión expiraba o se cerraba la app.", style_table_cell),
         Paragraph("<font color='#DC2626'><b>Alta</b></font>", style_table_cell_center),
         Paragraph("Tabla <code>borrador_producto</code> aislada por <code>cuentaClave</code> para persistir borrador reactivo en tiempo real.", style_table_cell),
         Paragraph("<font color='#166534'><b>CERRADO</b></font>", style_table_cell_center)],
         
        [Paragraph("<b>R8</b>", style_table_cell_center), Paragraph("Cierre de sesión y cambio de usuario", style_table_cell),
         Paragraph("Token se limpiaba pero SQLite no tenía aislamiento por cuenta, cruzando datos.", style_table_cell),
         Paragraph("<font color='#DC2626'><b>Crítica</b></font>", style_table_cell_center),
         Paragraph("Migración v2 a clave primaria compuesta <code>(cuentaClave, id)</code> en todas las tablas locales, garantizando aislamiento estricto por usuario.", style_table_cell),
         Paragraph("<font color='#166534'><b>CERRADO</b></font>", style_table_cell_center)]
    ]
    
    huecos_table = Table(huecos_data, colWidths=[24, 75, 135, 45, 180, 45])
    huecos_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_primary),
        ('BOX', (0,0), (-1,-1), 1, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('PADDING', (0,0), (-1,-1), 4),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, c_light_bg])
    ]))
    story.append(huecos_table)
    story.append(Spacer(1, 14))
    
    # -------------------------------------------------------------------------
    # SECCIÓN 2: ESQUEMA LOCAL Y CASO DE USO REFINADO (PASOS 2 Y 3)
    # -------------------------------------------------------------------------
    story.append(Paragraph("2. Esquema Local Ampliado y Caso de Uso Refinado (Pasos 2 y 3)", style_h1))
    story.append(Paragraph(
        "<b>Esquema SQLDelight (<code>Producto.sq</code>):</b> Para cumplir los requisitos de reintento, memoria de errores y aislamiento por cuenta, la tabla <code>producto</code> fue ampliada incorporando clave compuesta y trazabilidad exhaustiva de envío:",
        style_body
    ))
    
    sql_code = """CREATE TABLE producto (
    cuentaClave TEXT NOT NULL,
    id INTEGER NOT NULL,
    nombre TEXT NOT NULL,
    precio REAL NOT NULL,
    stock INTEGER NOT NULL,
    activo INTEGER,
    categoriaId INTEGER,
    categoriaNombre TEXT,
    datosIncompletos INTEGER NOT NULL DEFAULT 0,
    pendiente INTEGER NOT NULL DEFAULT 0,
    estadoEnvio TEXT NOT NULL DEFAULT 'CONFIRMADO',
    intentos INTEGER NOT NULL DEFAULT 0 CHECK(intentos >= 0),
    revision INTEGER NOT NULL DEFAULT 0,
    operacionLocalId TEXT,
    ultimoErrorCodigo TEXT,
    ultimoError TEXT,
    erroresCampos TEXT,
    proximoIntentoEnMs INTEGER,
    PRIMARY KEY(cuentaClave, id)
);"""
    sql_box = Table([[Paragraph(sql_code.replace("\n", "<br/>").replace(" ", "&nbsp;"), style_code)]], colWidths=[504])
    sql_box.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), c_light_bg),
        ('BOX', (0,0), (-1,-1), 1, c_border),
        ('PADDING', (0,0), (-1,-1), 6)
    ]))
    story.append(sql_box)
    story.append(Spacer(1, 10))
    
    story.append(Paragraph(
        "<b>Caso de Uso de Sincronización Refinado (<code>CoordinadorSincronizacion.kt</code>):</b> Se implementó la secuencia estricta solicitada: <b>subir pendientes antes de conciliar la descarga</b>. La descarga concilia todo el catálogo remoto de forma paginada en bloques de 20 elementos, consolidando el catálogo, la estampa de tiempo y el estado del ciclo dentro de una única transacción SQLite atómica.",
        style_body
    ))
    story.append(PageBreak())
    
    # -------------------------------------------------------------------------
    # SECCIÓN 3: PANTALLA PRINCIPAL CON RESUMEN DE SINCRONIZACIÓN (PASO 4)
    # -------------------------------------------------------------------------
    story.append(Paragraph("3. Pantalla Principal y Estado Observable de Sincronización (Paso 4)", style_h1))
    story.append(Paragraph(
        "El componente <code>ResumenSincronizacionCard</code> se diseñó como un elemento reactivo en la parte superior de <code>InicioScreen</code>. Obtiene su información combinando flujos observables locales de SQLite (<code>StateFlow</code>) sin realizar consultas a la red. Expone de manera clara al usuario:",
        style_body
    ))
    story.append(Paragraph(
        "• <b>Pendientes:</b> Total de operaciones creadas localmente esperando conexión.<br/>"
        "• <b>Fallidas / En revisión:</b> Filas que requirieron atención manual por fallo 422 o reintentos agotados.<br/>"
        "• <b>Antigüedad de datos:</b> Formato amigable relativo («Actualizado hace un momento», «hace 6 minutos»).<br/>"
        "• <b>Acciones contextuales:</b> Botón «Sincronizar» y botón «Revisar envíos» habilitado condicionalmente.",
        style_body
    ))
    story.append(Spacer(1, 8))
    
    img_e11_path = os.path.join(evidencias_dir, "E11-resumen-sincronizacion.png")
    if os.path.exists(img_e11_path):
        # Image table with side-by-side explanation
        img_e11 = Image(img_e11_path, width=160, height=355)
        e11_desc = """<b>Evidencia E11 — Captura Real en Android (AVD Pixel 7, API 34):</b><br/><br/>
<b>Elementos verificados en pantalla:</b><br/>
1. <b>Tarjeta de Sincronización:</b> Muestra contadores reales en tiempo de ejecución: <code>Pendientes: 1</code> y <code>En revisión: 1</code>.<br/><br/>
2. <b>Antigüedad de datos:</b> Indica <i>«Actualizado hace 6 minutos»</i> calculado a partir del epoch de la última descarga completa.<br/><br/>
3. <b>Acciones disponibles:</b> Se muestran los botones «Sincronizar» y «Revisar envíos» para acceder directamente a la gestión de las filas fallidas.<br/><br/>
4. <b>Banner Informativo del Permiso:</b> Con texto explicativo y botón contextual «Abrir Ajustes» al encontrarse el permiso en estado denegado definitivo.<br/><br/>
5. <b>Módulos Disponibles:</b> Sección informativa identificando la disponibilidad real de Productos y la condición de demostración de Clientes y Pedidos."""
        
        t_e11 = Table([[img_e11, Paragraph(e11_desc, style_body)]], colWidths=[180, 324])
        t_e11.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('BOX', (0,0), (-1,-1), 1, c_border),
            ('BACKGROUND', (0,0), (-1,-1), c_light_bg),
            ('PADDING', (0,0), (-1,-1), 8)
        ]))
        story.append(t_e11)
    
    story.append(PageBreak())
    
    # -------------------------------------------------------------------------
    # SECCIÓN 4: GESTOR DE PERMISOS Y SUS TRES ESTADOS (PASO 5)
    # -------------------------------------------------------------------------
    story.append(Paragraph("4. Gestor de Permisos Nativo y Resolución de los Tres Estados (Paso 5)", style_h1))
    story.append(Paragraph(
        "Se implementó el contrato común <code>GestorPermisos</code> en <code>commonMain</code>, desacoplado de las actividades de Android. En Android, <code>GestorPermisosAndroid</code> serializa solicitudes concurrentes mediante un mutex y <code>CompletableDeferred</code>, conectándose con <code>PuentePermisosActivity</code> sobre el contrato moderno de <code>ActivityResultContracts.RequestPermission()</code>. Soporta tanto API 33+ (<code>POST_NOTIFICATIONS</code>) como APIs anteriores (<code>NotificationManagerCompat.areNotificationsEnabled</code>).",
        style_body
    ))
    story.append(Spacer(1, 6))
    
    img_e12_d = os.path.join(evidencias_dir, "E12-permiso-denegado.png")
    img_e12_c = os.path.join(evidencias_dir, "E12-permiso-concedido.png")
    img_e12_a = os.path.join(evidencias_dir, "E12-permiso-ajustes.png")
    
    if os.path.exists(img_e12_d) and os.path.exists(img_e12_c) and os.path.exists(img_e12_a):
        perm_table_data = [
            [Image(img_e12_d, width=155, height=344),
             Image(img_e12_c, width=155, height=344),
             Image(img_e12_a, width=155, height=344)],
            [Paragraph("<b>Estado 1: Denegado / Inicial</b><br/>Banner con «Activar» y «Ahora no»", style_caption),
             Paragraph("<b>Estado 2: Concedido</b><br/>Banner desaparece automáticamente", style_caption),
             Paragraph("<b>Estado 3: Denegado Permanente</b><br/>Botón redirige a «Abrir Ajustes»", style_caption)],
            [Paragraph("Antes de solicitar el permiso del sistema, la aplicación explica de forma voluntaria para qué se requieren los avisos.", style_table_cell),
             Paragraph("Al conceder el permiso runtime, el banner se oculta y se habilita la publicación de notificaciones locales discretas.", style_table_cell),
             Paragraph("Si el usuario deniega con directiva 'No volver a preguntar' (USER_FIXED), el botón cambia a 'Abrir Ajustes' abriendo la configuración del SO.", style_table_cell)]
        ]
        perm_table = Table(perm_table_data, colWidths=[168, 168, 168])
        perm_table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
            ('BOX', (0,0), (-1,-1), 1, c_border),
            ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
            ('BACKGROUND', (0,0), (-1,-1), c_light_bg),
            ('PADDING', (0,0), (-1,-1), 5)
        ]))
        story.append(perm_table)
        
    story.append(PageBreak())
    
    # -------------------------------------------------------------------------
    # SECCIÓN 5: FIREBASE DETRÁS DE INTERFAZ Y REVISIÓN DE ENVÍOS (PASO 6)
    # -------------------------------------------------------------------------
    story.append(Paragraph("5. Módulo Firebase Desacoplado y Pantalla de Revisión de Envíos (Paso 6)", style_h1))
    story.append(Paragraph(
        "<b>Integración de Firebase Crashlytics (Paso 6):</b> Conforme al principio de inversión de dependencias, el módulo de dominio declara únicamente el puerto <code>Telemetria</code>. En la capa de datos Android se implementa <code>TelemetriaFirebase</code> envuelta obligatoriamente por <code>TelemetriaSegura</code>:",
        style_body
    ))
    story.append(Paragraph(
        "• <b>Sanitización Estricta:</b> Sustituye trazas de error por <code>ExcepcionTecnicaSanitizada</code> impidiendo la fuga de tokens JWT, correos o credenciales.<br/>"
        "• <b>Filtrado de Corrutinas:</b> Descarta <code>CancellationException</code> para evitar que cancelaciones normales de vistas cuenten como crashes.<br/>"
        "• <b>Verificación en Dispositivo Real:</b> Incorporación del archivo oficial <code>google-services.json</code> con Package Name <code>pe.edu.upeu.pharmamobile2</code> y Project ID <code>pharmamobile2</code>.",
        style_body
    ))
    
    logcat_snippet = """11-04 22:54:25.903  5306  5306 I FirebaseCrashlytics: Initializing Firebase Crashlytics 19.4.0 for pe.edu.upeu.pharmamobile2
11-04 22:54:26.374  5306  5336 I FirebaseCrashlytics: No version control information found
11-04 23:12:15.964  8346  8346 I FirebaseCrashlytics: Initializing Firebase Crashlytics 19.4.0 for pe.edu.upeu.pharmamobile2"""
    log_box = Table([[Paragraph("<b>Logcat Android Real (FirebaseCrashlytics:V):</b><br/>" + logcat_snippet.replace("\n", "<br/>").replace(" ", "&nbsp;"), style_code)]], colWidths=[504])
    log_box.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#0F172A")),
        ('BOX', (0,0), (-1,-1), 1, c_primary),
        ('TEXTCOLOR', (0,0), (-1,-1), colors.HexColor("#38BDF8")),
        ('PADDING', (0,0), (-1,-1), 6)
    ]))
    story.append(log_box)
    story.append(Spacer(1, 10))
    
    story.append(Paragraph("<b>Resolución Asistida de Errores (<code>RevisionEnviosScreen</code>):</b>", style_h2))
    img_e16_path = os.path.join(evidencias_dir, "E16-revision-envios.png")
    if os.path.exists(img_e16_path):
        img_e16 = Image(img_e16_path, width=160, height=355)
        e16_desc = """<b>Evidencia E16 — Pantalla de Revisión de Envíos:</b><br/><br/>
<b>Gestión individual por fila sin bloqueos:</b><br/>
1. <b>Fila en cola (PENDIENTE):</b> Muestra el producto creado offline (<code>Ibuprofeno 400mg Forte</code>) con 0 intentos y botón para descartar localmente.<br/><br/>
2. <b>Fila fallida (REVISION):</b> Muestra el producto <code>Amoxicilina 250mg Susp</code> con 5 intentos agotados y el error persistido exacto del backend:<br/>
<code>422 Unprocessable Entity: Lote duplicado</code>.<br/><br/>
3. <b>Decisión Asistida por el Usuario:</b> El usuario puede optar por <b>«Descartar»</b> la fila local o <b>«Reenviar»</b> asumiendo explícitamente el riesgo de duplicación frente a operaciones sin idempotencia en el servidor."""
        
        t_e16 = Table([[img_e16, Paragraph(e16_desc, style_body)]], colWidths=[180, 324])
        t_e16.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('BOX', (0,0), (-1,-1), 1, c_border),
            ('BACKGROUND', (0,0), (-1,-1), c_light_bg),
            ('PADDING', (0,0), (-1,-1), 8)
        ]))
        story.append(t_e16)
        
    story.append(PageBreak())
    
    # -------------------------------------------------------------------------
    # SECCIÓN 6: LISTA DE COTEJO OFICIAL (VERIFICACIÓN DE LOS 14 CRITERIOS)
    # -------------------------------------------------------------------------
    story.append(Paragraph("6. Lista de Cotejo Oficial (Verificación de los 14 Criterios)", style_h1))
    story.append(Paragraph(
        "Evaluación y verificación crítica de cada uno de los 14 criterios de la <b>Lista de Cotejo</b> oficial de la Guía Práctica N.º 13:",
        style_body
    ))
    
    cotejo_data = [
        [Paragraph("N.º", style_table_header), Paragraph("Criterio Observable de la Guía Práctica", style_table_header),
         Paragraph("Cumple", style_table_header), Paragraph("Justificación Técnica y Evidencia Comprobada", style_table_header)],
        
        [Paragraph("1", style_table_cell_center), Paragraph("Recorrí el flujo completo y registré por escrito los huecos encontrados.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Documentado en <code>docs/sesion13/01-recorrido-y-huecos.md</code> cubriendo los 8 momentos R1-R8 con gravedad y decisiones.", style_table_cell)],
         
        [Paragraph("2", style_table_cell_center), Paragraph("La tabla local guarda los intentos y el último error de cada fila pendiente.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Tabla <code>producto</code> ampliada con <code>intentos</code>, <code>ultimoError</code>, <code>ultimoErrorCodigo</code> y <code>revision</code>. Tests en <code>ColaProductoLocalTest</code>.", style_table_cell)],
         
        [Paragraph("3", style_table_cell_center), Paragraph("La sincronización distingue el fallo transitorio del permanente.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Jerarquía sellada <code>ResultadoEnvio</code> (Exito, Transitorio, Permanente) en dominio y mapeo en <code>PoliticaReintento.kt</code>. Tests <code>PoliticaReintentoTest</code>.", style_table_cell)],
         
        [Paragraph("4", style_table_cell_center), Paragraph("La espera entre intentos crece y existe un número máximo de intentos.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Backoff exponencial determinista (1s, 2s, 4s, 8s, 16s) y límite estricto de <code>MAX_INTENTOS = 5</code> verificado con tiempo virtual.", style_table_cell)],
         
        [Paragraph("5", style_table_cell_center), Paragraph("Una fila que agota los intentos queda marcada y visible para el usuario.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("La fila transita a <code>REVISION</code>, se contabiliza en el resumen de inicio y se gestiona en <code>RevisionEnviosScreen</code> (Evidencia E16).", style_table_cell)],
         
        [Paragraph("6", style_table_cell_center), Paragraph("La subida ocurre antes de la bajada y nada se pierde al sincronizar.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("<code>CoordinadorSincronizacion</code> garantiza vaciar pendientes antes de realizar la descarga atómica paginada sin solapamiento.", style_table_cell)],
         
        [Paragraph("7", style_table_cell_center), Paragraph("La pantalla principal muestra pendientes, fallidas y antigüedad de los datos.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Componente <code>ResumenSincronizacionCard</code> en <code>InicioScreen</code> con flujos observables sin consultar red (Evidencia E11).", style_table_cell)],
         
        [Paragraph("8", style_table_cell_center), Paragraph("El gestor de permisos está declarado como interfaz en el código común.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Interfaz <code>GestorPermisos</code> en <code>commonMain/domain/port</code> con enum <code>Permiso</code> (CAMARA, UBICACION, NOTIFICACIONES) y <code>EstadoPermiso</code>.", style_table_cell)],
         
        [Paragraph("9", style_table_cell_center), Paragraph("La implementación de Android distingue los tres estados del permiso.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Implementado en <code>GestorPermisosAndroid</code> con SharedPreferences y rationale. Capturas de los tres estados: E12-D, E12-C y E12-A.", style_table_cell)],
         
        [Paragraph("10", style_table_cell_center), Paragraph("El permiso está declarado en el manifiesto y, si aplica, la clave está en Info.plist.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Declarado <code>android.permission.POST_NOTIFICATIONS</code> en <code>AndroidManifest.xml</code>. Adaptadores iOS documentados en <code>06-verificacion-ios-pendiente.md</code>.", style_table_cell)],
         
        [Paragraph("11", style_table_cell_center), Paragraph("La interfaz explica antes de pedir y ofrece ir a Ajustes cuando corresponde.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Banner contextual voluntario en tarjeta de sincronización con botón «Activar» o botón «Abrir Ajustes» según estado (Evidencias E12-D y E12-A).", style_table_cell)],
         
        [Paragraph("12", style_table_cell_center), Paragraph("El SDK de Firebase no aparece fuera de la capa de datos.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Dominio y presentación 100% agnósticos de Firebase. SDK encapsulado en <code>TelemetriaFirebase</code> y sanitizado con <code>TelemetriaSegura</code>.", style_table_cell)],
         
        [Paragraph("13", style_table_cell_center), Paragraph("Las pruebas de commonTest se ejecutan y pasan.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("116 pruebas automatizadas ejecutadas en <code>:shared:testAndroidHostTest</code> con 0 fallos y 0 errores.", style_table_cell)],
         
        [Paragraph("14", style_table_cell_center), Paragraph("Los commits siguen la convención y están distribuidos a lo largo de la sesión.", style_table_cell),
         Paragraph("<font color='#166534'><b>SÍ</b></font>", style_table_cell_center),
         Paragraph("Rama <code>feature/integrador-13</code> con commits convencionales trazables para cada paso (Paso 1 al Paso 7) sincronizada en GitHub.", style_table_cell)]
    ]
    
    cotejo_table = Table(cotejo_data, colWidths=[20, 185, 45, 254])
    cotejo_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_primary),
        ('BOX', (0,0), (-1,-1), 1, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('PADDING', (0,0), (-1,-1), 4),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, c_light_bg])
    ]))
    story.append(cotejo_table)
    story.append(Spacer(1, 14))
    
    # -------------------------------------------------------------------------
    # SECCIÓN 7: TRAZABILIDAD DE COMMITS Y CONCLUSIÓN
    # -------------------------------------------------------------------------
    story.append(Paragraph("7. Trazabilidad de Commits y Enlaces de Auditoría", style_h1))
    
    commits_data = [
        [Paragraph("Paso / Tarea", style_table_header), Paragraph("Hash Commit", style_table_header),
         Paragraph("Mensaje Convencional de Commit", style_table_header), Paragraph("Resultado de Verificación", style_table_header)],
        [Paragraph("Paso 1", style_table_cell_bold), Paragraph("<code>21ba2a3</code>", style_table_cell), Paragraph("docs(s13): registrar plan entorno y brechas verificadas", style_table_cell), Paragraph("8 huecos auditados", style_table_cell)],
        [Paragraph("Paso 2", style_table_cell_bold), Paragraph("<code>aaf9146</code>", style_table_cell), Paragraph("feat(local): migrar catalogo y persistir estado de envio por cuenta", style_table_cell), Paragraph("Migración v2 SQLDelight", style_table_cell)],
        [Paragraph("Paso 3", style_table_cell_bold), Paragraph("<code>bd019d6</code>", style_table_cell), Paragraph("feat(sync): cerrar caso de uso de sincronizacion y aislamiento de cuenta", style_table_cell), Paragraph("Coordinador atómico", style_table_cell)],
        [Paragraph("Paso 4", style_table_cell_bold), Paragraph("<code>b657014</code>", style_table_cell), Paragraph("feat(ui): presentar resumen de sincronizacion pendientes y fallidas en inicio", style_table_cell), Paragraph("Card observable en Inicio", style_table_cell)],
        [Paragraph("Paso 5", style_table_cell_bold), Paragraph("<code>a6c48b8</code>", style_table_cell), Paragraph("feat(native): integrar gestor de permisos y notificaciones locales", style_table_cell), Paragraph("3 estados Android resueltos", style_table_cell)],
        [Paragraph("Paso 6", style_table_cell_bold), Paragraph("<code>3539165</code>", style_table_cell), Paragraph("feat(telemetria): integrar firebase crashlytics bajo interfaz de dominio", style_table_cell), Paragraph("Telemetría sanitizada", style_table_cell)],
        [Paragraph("Paso 7", style_table_cell_bold), Paragraph("<code>bc21ca0</code>", style_table_cell), Paragraph("docs(s13): registrar informe final y evidencias de ejecucion", style_table_cell), Paragraph("Evidencias y matrices", style_table_cell)],
        [Paragraph("Alineación", style_table_cell_bold), Paragraph("<code>22667a0</code>", style_table_cell), Paragraph("feat(permisos): alinear enum Permiso con la especificacion oficial", style_table_cell), Paragraph("Enum CAMARA/UBIC/NOTIF", style_table_cell)]
    ]
    commits_table = Table(commits_data, colWidths=[65, 65, 250, 124])
    commits_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_secondary),
        ('BOX', (0,0), (-1,-1), 1, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('PADDING', (0,0), (-1,-1), 4),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, c_light_bg])
    ]))
    story.append(commits_table)
    story.append(Spacer(1, 12))
    
    story.append(Paragraph(
        "<b>Conclusión de la Auditoría:</b> La aplicación móvil PharmaMobil cumple de forma rigurosa y verificable con la totalidad de los criterios académicos y arquitectónicos de la Guía Práctica N.º 13. El repositorio remoto en GitHub refleja la sincronización íntegra de la rama <code>feature/integrador-13</code>, quedando listo para la sustentación y evaluación de fin de curso.",
        style_body
    ))
    
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"PDF successfully generated at: {pdf_path}")

if __name__ == "__main__":
    build_pdf()
