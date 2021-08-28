package com.company;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Scanner;

public class Main {

    static Scanner scanner = new Scanner(System.in);
    static int ApiType = 0;
    static int Direction = 0; // 0 horizontal or vertical
    static int[] options = new int[] {3, 5, 11, 21};
    static double[][] kernels = new double[][] {
            {0.27901, 0.44198, 0.27901},
            {0.06136, 0.24477, 0.38774, 0.24477, 0.06136},
            {0.000003, 0.000229, 0.005977 ,0.060598, 0.24173, 0.382925, 0.24173, 0.060598, 0.005977, 0.000229, 0.000003},
            {0.011254, 0.016436, 0.023066, 0.031105, 0.040306, 0.050187, 0.060049, 0.069041, 0.076276, 0.080977, 0.082607, 0.080977, 0.076276, 0.069041, 0.060049, 0.050187, 0.040306, 0.031105, 0.023066, 0.016436, 0.011254
            }
    };
    static int option = 0;
    static boolean GenerateFragmentShader = true;

    static int GetShaderType()
    {
        System.out.println("[0] OpenGL Shader");
        System.out.println("[1] Vulkan Shader");
        System.out.print("Shader Type: ");
        try {
            int type = scanner.nextInt();
            if(type == 0 || type == 1)
                return type;
            return GetShaderType();
        } catch(Exception e) {
            scanner = new Scanner(System.in);
            return GetShaderType();
        }
    }

    static int GetDirectionType()
    {
        System.out.println("[0] Horizontal Gaussian Blur");
        System.out.println("[1] Vertical Gaussian Blur");
        System.out.print("Direction Type: ");
        try {
            int direction = scanner.nextInt();
            if(direction == 0 || direction == 1)
                return direction;
            return GetShaderType();
        } catch(Exception e) {
            scanner = new Scanner(System.in);
            return GetDirectionType();
        }
    }

    static int GetOptions()
    {
        for(int i = 0; i < options.length; i++)
        {
            System.out.println("[" + i + "] Kernel Size " + options[i]);
        }
        System.out.print("Kernel Size: ");
        try {
            int option = scanner.nextInt();
            if(option >= 0 && option < options.length)
                return option;
            return GetOptions();
        } catch(Exception e) {
            scanner = new Scanner(System.in);
            return GetOptions();
        }
    }

    static boolean GetFragmentShader() {
        System.out.println("Generate Fragment Shader");
        System.out.println("(NOTE) If both horizontal and vertical gaussian blur pass use the same kernel size, then you can use the same fragment shader for both!");
        System.out.println("[0] YES");
        System.out.println("[1] NO");
        System.out.print("Generate: ");
        try {
            int ge = scanner.nextInt();
            if(ge == 0 || ge == 1)
                return ge == 0;
            return GetFragmentShader();
        } catch(Exception e) {
            scanner = new Scanner(System.in);
            return GetFragmentShader();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Gaussian Blur GLSL Shader Generator");
        ApiType = GetShaderType();
        System.out.println(ApiType == 0 ? "==========OpenGL==========" : "==========Vulkan==========");
        Direction = GetDirectionType();
        System.out.println(Direction == 0 ? "==========HORIZONTAL==========" : "==========VERTICAL==========");
        option = GetOptions();
        System.out.println("==========KERNEL SIZE " + options[option] + "==========");
        GenerateFragmentShader = GetFragmentShader();
        /* Generate Vertex Shader */
        if(ApiType == 1) {
            String vulkan_vertex_code = GenerateVulkanShader();
            String filename = "gaussian_" + ((Direction == 0) ? "horizontal" : "vertical") + "_" + options[option] + "_kernel.vert";
            BufferedWriter output = new BufferedWriter(new FileWriter(filename));
            output.write(vulkan_vertex_code);
            output.flush();
            output.close();
            System.out.println("Output (Vulkan) Vertex Shader ---> " + filename);
            if(GenerateFragmentShader) {
                String vulkan_fragment_code = GenerateVulkanFragmentShader();
                String fragment_filename = "gaussian_kernel_" + options[option] + "_blur.frag";
                output = new BufferedWriter(new FileWriter(fragment_filename));
                output.write(vulkan_fragment_code);
                output.flush();
                output.close();
                System.out.println("Output (Vulkan) Fragment Shader ---> " + fragment_filename);
            }
        }
        else {
            String opengl_vertex_code = GenerateOpenGLShader();
            String filename = "gaussian_" + ((Direction == 0) ? "horizontal" : "vertical") + "_" + options[option] + "_kernel.glsl";
            BufferedWriter output = new BufferedWriter(new FileWriter(filename));
            output.write(opengl_vertex_code);
            output.flush();
            output.close();
            System.out.println("Output (OpenGL) Vertex Shader ---> " + filename);
            if(GenerateFragmentShader) {
                String opengl_fragment_code = GenerateOpenGLFragmentShader();
                String fragment_filename = "gaussian_kernel_" + options[option] + "_blur.glsl";
                output = new BufferedWriter(new FileWriter(fragment_filename));
                output.write(opengl_fragment_code);
                output.flush();
                output.close();
                System.out.println("Output (OpenGL) Fragment Shader ---> " + fragment_filename);
            }
        }
    }

    static String GenerateOpenGLShader()
    {
        String code = "#version 330 core\n" +
                "\n" +
                "const vec4 quad[] = \n" +
                "{\n" +
                "    // triangle 1\n" +
                "    vec4(-1, 1, 0, 1),\n" +
                "    vec4(1, -1, 1, 0),\n" +
                "    vec4(-1, -1, 0, 0),\n" +
                "    // triangle 2\n" +
                "    vec4(-1, 1, 0, 1),\n" +
                "    vec4(1, 1, 1, 1),\n" +
                "    vec4(1, -1, 1, 0)\n" +
                "};\n" +
                "\n" +
                "#define KERNEL_SIZE " + options[option] + "\n" +
                "out vec2 TexCoord[KERNEL_SIZE];\n" +
                "uniform float "
                ;
        if(Direction == 0) {
            // horizontal
            code += "TextureWidth;\n";
        } else {
            // vertical
            code += "TextureHeight;\n";
        }
        code += "\n" +
                "void main()\n" +
                "{\n" +
                "    vec4 Vertex = quad[gl_VertexIndex];\n" +
                "    gl_Position = vec4(Vertex.xy, 0.0, 1.0);\n" +
                "    vec2 CenteredTextureCoord = Vertex.zw;\n" +
                "    float PixelSize = 1.0 / ";
        if(Direction == 0) {
            // horizontal
            code += "TextureWidth;\n";
        } else {
            // vertical
            code += "TextureHeight;\n";
        }
        code += "\n" +
                "    for(int i = -KERNEL_SIZE; i <= KERNEL_SIZE; i++)\n" +
                "    {\n" +
                "        TexCoord[i + KERNEL_SIZE/2] = CenteredTextureCoord + vec2(0.0, PixelSize * i);\n" +
                "    }\n" +
                "}\n";
        return code;
    }

    static String GenerateVulkanShader()
    {
        String code = "#version 450 core\n" +
                "#define KERNEL_SIZE " + options[option] + "\n" +
                "const vec4 quad[] = \n" +
                "{\n" +
                "    // triangle 1\n" +
                "    vec4(-1, 1, 0, 1),\n" +
                "    vec4(1, -1, 1, 0),\n" +
                "    vec4(-1, -1, 0, 0),\n" +
                "    // triangle 2\n" +
                "    vec4(-1, 1, 0, 1),\n" +
                "    vec4(1, 1, 1, 1),\n" +
                "    vec4(1, -1, 1, 0)\n" +
                "};\n" +
                "layout (location = 0) out vec2 TexCoord[KERNEL_SIZE];\n" +
                "layout (push_constant) uniform constants\n" +
                "{\n"
                ;
        if(Direction == 0) {
            // horizontal
            code += "    float TextureWidth;\n};\n";

        } else {
            // vertical
            code += "    float TextureHeight;\n};\n";
        }
        code += "\nvoid main()\n" +
                "{\n" +
                "    vec4 Vertex = quad[gl_VertexIndex];\n" +
                "    gl_Position = vec4(Vertex.xy, 0.0, 1.0);\n";
        if(Direction == 0) {
            // horizontal
            code += "    float PixelSize = 1.0 / TextureWidth;\n";
            code += "    vec2 CenteredTextureCoord = Vertex.zw;\n" +
                    "\n" +
                    "    for(int i = -KERNEL_SIZE/2; i <= KERNEL_SIZE/2; i++)\n" +
                    "    {\n" +
                    "        TexCoord[i + KERNEL_SIZE/2] = CenteredTextureCoord + vec2(PixelSize * i, 0.0);\n" +
                    "    }\n" +
                    "}";
        } else {
            // vertical
            code += "    float PixelSize = 1.0 / TextureHeight;\n";
            code += "    vec2 CenteredTextureCoord = Vertex.zw;\n" +
                    "\n" +
                    "    for(int i = -KERNEL_SIZE/2; i <= KERNEL_SIZE/2; i++)\n" +
                    "    {\n" +
                    "        TexCoord[i + KERNEL_SIZE/2] = CenteredTextureCoord + vec2(0.0, PixelSize * i);\n" +
                    "    }\n" +
                    "}";
        }
        return code;
    }

    static String GenerateOpenGLFragmentShader()
    {
        String code = "#version 330 core\n" +
                "#define KERNEL_SIZE " + options[option] + "\n" +
                "\n" +
                "in vec2 TexCoord[KERNEL_SIZE];\n" +
                "uniform sampler2D TargetTexture;\n" +
                "out vec4 FragColor;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tFragColor = vec4(0.0);\n"
                ;
        for(int i = 0; i < options[option]; i++) {
            code += "\tFragColor += texture(TargetTexture, TexCoord[" + i + "]) * " + kernels[option][i] + ";\n";
        }
        code += "}\n";
        return code;
    }

    static String GenerateVulkanFragmentShader()
    {
        String code = "#version 450 core\n" +
                "#define KERNEL_SIZE " + options[option] + "\n" +
                "layout (location = 0) in vec2 TexCoord[KERNEL_SIZE];\n" +
                "layout(set = 0, binding = 0) uniform sampler2D TargetTexture;\n" +
                "layout (location = 0) out vec4 FragColor;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tFragColor = vec4(0.0);\n"
                ;
        for(int i = 0; i < options[option]; i++) {
            code += "\tFragColor += texture(TargetTexture, TexCoord[" + i + "]) * " + kernels[option][i] + ";\n";
        }
        code += "}\n";
        return code;
    }
}
