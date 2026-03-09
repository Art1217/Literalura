package com.alura.literalura.principal;

import com.alura.literalura.model.*;
import com.alura.literalura.repository.AutorRepository;
import com.alura.literalura.repository.LibroRepository;
import com.alura.literalura.service.ConsumoAPI;
import com.alura.literalura.service.ConvierteDatos;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Principal {
    private Scanner lectura = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://gutendex.com/books/";
    private ConvierteDatos conversor = new ConvierteDatos();
    private LibroRepository libroRepository;
    private AutorRepository autorRepository;

    public Principal(LibroRepository libroRepository, AutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar libro por título
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4 - Listar autores vivos en un determinado año
                    5 - Listar libros por idioma
                    6 - Top 10 libros más descargados

                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = lectura.nextInt();
            lectura.nextLine();

            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    mostrarLibrosRegistrados();
                    break;
                case 3:
                    mostrarAutoresRegistrados();
                    break;
                case 4:
                    mostrarAutoresVivos();
                    break;
                case 5:
                    mostrarLibrosPorIdioma();
                    break;
                case 6:
                    mostrarTop10();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }

    private DatosLibro getDatosLibro() {
        System.out.println("Escribe el nombre del libro que deseas buscar");
        var nombreLibro = lectura.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + "?search=" + nombreLibro.replace(" ", "+"));
        var datosBusqueda = conversor.obtenerDatos(json, RespuestaAPI.class);
        Optional<DatosLibro> libroBuscado = datosBusqueda.resultados().stream()
                .filter(l -> l.titulo().toUpperCase().contains(nombreLibro.toUpperCase()))
                .findFirst();
        if (libroBuscado.isPresent()) {
            return libroBuscado.get();
        } else {
            System.out.println("Libro no encontrado");
            return null;
        }
    }

    private void buscarLibroPorTitulo() {
        DatosLibro datos = getDatosLibro();
        if (datos != null) {
            Libro libro = new Libro(datos);

            // Handle Author
            if (!datos.autor().isEmpty()) {
                DatosAutor datosAutor = datos.autor().get(0);
                Optional<Autor> autorExistente = autorRepository.findByNombreIgnoreCase(datosAutor.nombre());
                if (autorExistente.isPresent()) {
                    libro.setAutor(autorExistente.get());
                } else {
                    Autor nuevoAutor = new Autor(datosAutor);
                    autorRepository.save(nuevoAutor);
                    libro.setAutor(nuevoAutor);
                }
            }

            try {
                libroRepository.save(libro);
                System.out.println(libro);
            } catch (Exception e) {
                System.out.println("El libro ya se encuentra registrado.");
            }
        }
    }

    private void mostrarLibrosRegistrados() {
        List<Libro> libros = libroRepository.findAll();
        libros.forEach(System.out::println);
    }

    private void mostrarAutoresRegistrados() {
        List<Autor> autores = autorRepository.findAll();
        autores.forEach(a -> {
            String libros = a.getLibros().stream()
                    .map(Libro::getTitulo)
                    .collect(Collectors.joining(", "));
            System.out.println("Autor: " + a.getNombre() + "\nFecha de nacimiento: " + a.getFechaDeNacimiento() +
                    "\nFecha de fallecimiento: " + a.getFechaDeFallecimiento() + "\nLibros: [" + libros + "]\n");
        });
    }

    private void mostrarAutoresVivos() {
        System.out.println("Ingresa el año que deseas buscar:");
        var anio = lectura.nextInt();
        lectura.nextLine();
        List<Autor> autoresVivos = autorRepository.buscarAutoresVivosEnAnio(anio);
        if (autoresVivos.isEmpty()) {
            System.out.println("No hay autores registrados vivos en ese año.");
        } else {
            autoresVivos.forEach(System.out::println);
        }
    }

    private void mostrarLibrosPorIdioma() {
        System.out.println("""
                Elija el idioma:
                es - español
                en - inglés
                fr - francés
                pt - portugués
                """);
        var idioma = lectura.nextLine();
        List<Libro> librosPorIdioma = libroRepository.findByIdiomaContains(idioma);
        if (librosPorIdioma.isEmpty()) {
            System.out.println("No hay libros registrados en ese idioma.");
        } else {
            librosPorIdioma.forEach(System.out::println);
        }
    }

    private void mostrarTop10() {
        List<Libro> topLibros = libroRepository.findTop10ByOrderByNumeroDeDescargasDesc();
        topLibros.forEach(System.out::println);
    }
}
