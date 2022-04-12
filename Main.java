import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Scanner;

// ******************************************************
// Author: Eric Schmidt
// Homework 2
// Date: 4/6/2022
// 
// Description:
// -HYPO machine simulator as well as the MTOPS real-time
// multitasking operating system
// -Contains global variables to simulate the hardware for the
// HYPO architecture as well as the MTOPS OS
// -Contains the following functions:
// ---main
// ---InitializeSystem
// ---AbsoluteLoader
// ---CPU
// ---FetchOperand
// ---DumpMemory
// ---CreateProcess
// ---InitializePCB
// ---PrintPCB
// ---PrintQueue
// ---InsertIntoRQ
// ---InsertIntoWQ
// ---SelectProcessFromRQ
// ---SaveContext
// ---Dispatcher
// ---TerminateProcess
// ---AllocateOSMemory
// ---FreeOSMemory
// ---AllocateUserMemory
// ---FreeUserMemory
// ---CheckAndProcessInterrupt
// ---ISRrunProgramInterrupt
// ---ISRinputCompletionInterrupt
// ---ISRoutputCompletionInterrupt
// ---ISRshutdownSystem
// ---SearchAndRemovePCBfromWQ
// ---SearchRQ
// ---SystemCall
// ---MemAllocSystemCall
// ---MemFreeSystemCall
// ---io_getcSystemCall
// ---io_putcSystemCall
// ******************************************************

class Main {
    // HYPO Architecture Globals
    long memory[] = new long[10000];
    long MAR;
    long MBR;
    long clock;
    long GPRs[] = new long[8];
    long IR;
    long PSR;
    long PC;
    long SP;

    // Constants
    final int END_OF_PROGRAM = -1;
    final int PROGRAM_AREA_END = 2999;
    final int PROGRAM_AREA_START = 0;
    final int USER_DYNAMIC_MEMORY_END = 6999;
    final int USER_DYNAMIC_MEMORY_START = 3000;
    final int OS_MEMORY_END = 9999;
    final int OS_MEMORY_START = 7000;
    final int AMOUNT_MODES = 6;
    final int AMOUNT_OPCODES = 12;
    final int REGISTER_MODE = 1;
    final int IMMEDIATE_MODE = 6;
    final int HALT_TIME = 12;
    final int ADD_TIME = 3;
    final int SUBTRACT_TIME = 3;
    final int MULTIPLY_TIME = 6;
    final int DIVIDE_TIME = 6;
    final int MOVE_TIME = 2;
    final int BRANCH_TIME = 2;
    final int BRANCH_M_TIME = 4;
    final int BRANCH_P_TIME = 4;
    final int BRANCH_Z_TIME = 4;
    final int PUSH_TIME = 2;
    final int POP_TIME = 2;
    final int SYSTEM_CALL_TIME = 12;

    // HYPO Status Codes
    final int OK = 0;
    final int Halt = 1;
    final int ErrorFileOpen = -2;
    final int InvalidAddress = -3;
    final int NoEndOfProgram = -4;
    final int InvalidPCValue = -5;
    final int InvalidMode = -6;
    final int InvalidGPRNumber = -7;
    final int InvalidOpcode = -8;
    final int FetchError = -9;
    final int ImmediateModeError = -10;
    final int DivisionByZero = -11;
    final int StackOverflow = -12;
    final int StackUnderflow = -13;

    // Output to file object
    static PrintStream out;

    // Class to return object from FetchOperand Method
    public class Output {
        private long OpAddress;
        private long OpValue;
        private int status;

        public Output(long address, long value, int status) {
            OpAddress = address;
            OpValue = value;
            this.status = status;
        }

        public void setOpAddress(long address) {
            OpAddress = address;
        }

        public void setOpValue(long value) {
            OpValue = value;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public long getOpAddress() {
            return OpAddress;
        }

        public long getOpValue() {
            return OpValue;
        }

        public int getStatus() {
            return status;
        }
    }

    // Start of homework 2 additions
    Scanner input = new Scanner(System.in);
    boolean ProgramHalt = false;
    long status = 0;
    long TimeSliceExpired = 4;
    long RunningPCBptr;
    final int EOL = -1;
    final int StackSize = 10;
    boolean SystemShutdownStatus = false;
    final long TimeSlice = 200;
    final int StartOfInputOperation = 2;
    final int StartOfOutputOperation = 3;

    // OSFree and UserFree list fields
    final int NextUserFreeListIndex = 0;
    final int UserFreeListSizeIndex = 1;
    final int NextOSFreeListIndex = 0;
    final int OSFreeListSizeIndex = 1;

    // Modes
    final int OSMode = 1;
    final int UserMode = 2;

    // Status Codes
    final int NoFreeMemory = -14;
    final int InvalidMemorySize = -15;
    final int InvalidMemoryAddress = -16;
    final int MemoryAllocationFailed = -17;
    final int LoadingProgramFailed = -18;

    // Link lists
    long RQ = EOL;
    long WQ = EOL;
    long OSFreeList = EOL;
    long UserFreeList = EOL;

    // States
    long ReadyState = 1;
    long RunningState = 2;
    long WaitingState = 3;

    // PCB globals
    long ProcessID = 1;
    int PCBsize = 22;
    long DefaultPriority = 128;

    // PCB fields
    final int NextPCBindex = 0;
    final int PIDindex = 1;
    final int StateIndex = 2;
    final int ReasonIndex = 3;
    final int PriorityIndex = 4;
    final int StackStartIndex = 5;
    final int StackSizeIndex = 6;
    final int GPR0Index = 11;
    final int GPR1Index = 12;
    final int GPR2Index = 13;
    final int GPR3Index = 14;
    final int GPR4Index = 15;
    final int GPR5Index = 16;
    final int GPR6Index = 17;
    final int GPR7Index = 18;
    final int SPindex = 19;
    final int PCindex = 20;
    final int PSRindex = 21;

    // ******************************************************
    // function: main
    //
    // Task Description:
    // -Initialize the system
    // -Get input from the user to choose a file to load
    // -Load the file into memory
    // -Dump memory before program execution
    // -Run the CPU
    // -Dump the memory after execution is complete
    //
    // Input parameters
    // -None
    //
    // Function return value
    // -None
    // ******************************************************
    public static void main(String[] args) {
        Main main = new Main();
        try {
            Main.out = new PrintStream(new FileOutputStream("output.txt"));
        } catch (Exception e) {
            System.out.println("File Error");
        }

        // Run until shutdown

        main.InitializeSystem(out);

        while (!main.SystemShutdownStatus) {
            // Check and process interrupt
            if (!(main.status == main.TimeSliceExpired)) {
                main.CheckAndProcessInterrupt(out);
            }
            if (main.SystemShutdownStatus) {
                System.out.println("OS is shutting down...");
                out.println("OS is shutting down...");
                return;
            }

            // Dump RQ and WQ
            System.out.println("RQ: Before CPU scheduling");
            out.println("RQ: Before CPU scheduling");
            main.PrintQueue(out, main.RQ);
            System.out.println();
            out.println();

            System.out.println("WQ: Before CPU scheduling");
            out.println("WQ: Before CPU scheduling");
            main.PrintQueue(out, main.WQ);
            System.out.println();
            out.println();

            main.DumpMemory(out, "Dynamic Memory Area before CPU scheduling", main.USER_DYNAMIC_MEMORY_START, 200);
            System.out.println();
            out.println();

            // Select next process from RQ to give CPU
            main.RunningPCBptr = main.SelectProcessFromRQ();

            // Perform restore context using Dispatcher
            main.Dispatcher(main.RunningPCBptr);

            System.out.println("RQ: After selecting process from RQ");
            out.println("RQ: After selecting process from RQ");
            main.PrintQueue(out, main.RQ);
            System.out.println();
            out.println();

            System.out.println("Running PCB:");
            out.println("Running PCB:");
            main.PrintPCB(out, main.RunningPCBptr);
            System.out.println();
            out.println();

            // Execute instructions of the running process using CPU
            main.status = main.CPU(out);

            // Dump dynamic memory area
            main.DumpMemory(out, "After execute program", main.USER_DYNAMIC_MEMORY_START, 200);
            System.out.println();
            out.println();

            // Check return status - reason for giving up CPU
            if (main.status == main.TimeSliceExpired) {
                main.SaveContext(main.RunningPCBptr); // Running process is losing CPU
                main.InsertIntoRQ(out, main.RunningPCBptr);
                main.RunningPCBptr = main.EOL;
            } else if (main.status == main.Halt || main.status < 0) {
                // Halt or runtime error
                main.TerminateProcess(out, main.RunningPCBptr);
                main.RunningPCBptr = main.EOL;
            } else if (main.status == main.StartOfInputOperation) {
                // io_getc
                main.memory[(int) main.RunningPCBptr + main.ReasonIndex] = main.StartOfInputOperation;
                main.SaveContext(main.RunningPCBptr); // Running process is losing CPU
                main.InsertIntoWQ(out, main.RunningPCBptr);
                main.RunningPCBptr = main.EOL;
            } else if (main.status == main.StartOfOutputOperation) {
                // io_putc
                main.memory[(int) main.RunningPCBptr + main.ReasonIndex] = main.StartOfOutputOperation;
                main.SaveContext(main.RunningPCBptr); // Running process is losing CPU
                main.InsertIntoWQ(out, main.RunningPCBptr);
                main.RunningPCBptr = main.EOL;
            } else {
                // Unknown programming error
                System.out.println("Error: Unknown Programming Error");
                out.println("Error: Unknown Programming Error");
            }
        } // end of while
        return;
    } // end of main() function

    // ************************************************************
    // Function: InitializeSystem
    //
    // Task Description:
    // -Set all global system hardware components to 0
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void InitializeSystem(PrintStream out) {
        // Initialize all Hypo Machine hardware components to zero
        Arrays.fill(memory, 0);
        MAR = 0;
        MBR = 0;
        clock = 0;
        Arrays.fill(GPRs, 0);
        IR = 0;
        PSR = 0;
        PC = 0;
        SP = 0;

        // Create User free list using the free block address and size
        UserFreeList = USER_DYNAMIC_MEMORY_START;
        memory[(int) UserFreeList + NextUserFreeListIndex] = EOL;
        memory[(int) UserFreeList + UserFreeListSizeIndex] = 300;

        // Create OS free list using the free block address and size
        OSFreeList = OS_MEMORY_START;
        memory[(int) OSFreeList + NextOSFreeListIndex] = EOL;
        memory[(int) OSFreeList + OSFreeListSizeIndex] = 300;

        CreateProcess(out, "NULL.txt", 0);
    } // end of InitializeSystem function

    // ********************************************************************
    // Function: AbsoluteLoader
    //
    // Task Description:
    // -Open the file containing HYPO machine user program and
    // load the content into HYPO memory
    // -On successful load, return the PC value in the End of Program line
    // -On failure, display appropriate error message and return appropriate error
    // code
    //
    // Input Parameters
    // -Out: Print to file object
    // -Filename: Name of the Hypo Machine executable file
    //
    // Function Return Values
    // -InvalidAddress status code
    // -NoEndOfProgram status code
    // -ErrorFileOpen status code
    // -Content for PC
    // ********************************************************************
    public long AbsoluteLoader(PrintStream out, String filename) {
        try {
            // Load the program from the given filename into HYPO Memory
            File file = new File(filename);
            Scanner fileReader = new Scanner(file);

            long address;
            long content;

            // Read from file until end of program or end of file and
            // store program in HYPO memory
            while (fileReader.hasNext()) {
                address = fileReader.nextLong();
                content = fileReader.nextLong();

                if (address == END_OF_PROGRAM) {
                    fileReader.close();
                    if (content >= PROGRAM_AREA_START && content <= PROGRAM_AREA_END) {
                        return content;
                    } else {
                        System.out.println("Error: Invalid Address Range");
                        out.println("Error: Invalid Address Range");
                        return InvalidAddress;
                    }
                } else if (address >= PROGRAM_AREA_START && address <= PROGRAM_AREA_END) {
                    memory[(int) address] = content;
                } else {
                    System.out.println("Error: Invalid Address Range");
                    out.println("Error: Invalid Address Range");
                    fileReader.close();
                    return InvalidAddress;
                }
            } // end of while loop

            // End of file encountered without End of Program line
            System.out.println("Error: No End of Program");
            out.println("Error: No End of Program");
            fileReader.close();
            return NoEndOfProgram;
        } catch (FileNotFoundException e) {
            System.out.println("Error: File Not Found");
            out.println("Error: File Not Found");
            return ErrorFileOpen;
        }
    } // end of AbsoluteLoader function

    // ************************************************************
    // Function: CPU
    //
    // Task Description:
    // -Parse instruction to get opcode, op1mode, op1gpr, op2mode,
    // op2gpr
    // -Based on opcode, do one of the following:
    // ---Halt
    // ---Add
    // ---Subtract
    // ---Multiply
    // ---Divide
    // ---Move
    // ---Branch
    // ---BrOnMinus
    // ---BrOnPlus
    // ---BrOnZero
    // ---Push
    // ---Pop
    // ---System Call
    // -Check for mode, address, and fetch errors
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -Status Code
    // -InvalidGprNumber
    // -InvalidMode
    // -ImmediateModeError
    // -FetchError
    // -DivisionByZero
    // -InvalidPCValue
    // -StackOverflow
    // -StackUnderflow
    // -InvalidOpcode
    // ************************************************************
    public long CPU(PrintStream out) {
        long Opcode;
        long Remainder;
        long Op1Mode;
        long Op1Gpr;
        long Op2Mode;
        long Op2Gpr;
        Output out1;
        Output out2;
        long result;
        long TimeLeft = TimeSlice;

        // Set or reset status to OK
        status = OK;
        // Set or reset ProgramHalt to false
        ProgramHalt = false;

        while (!ProgramHalt && status == OK && TimeLeft > 0) {
            // Fetch Cycle
            // Fetch (read) the first word of the instruction pointed by PC into MBR
            // Instruction needing more words (2 word and 3 word instructions) are fetched
            // based on instruction (opcode)
            // when the operand 1 and operand 2 values are fetched using modes

            if (PC >= PROGRAM_AREA_START && PC <= PROGRAM_AREA_END) {
                MAR = PC;
                PC++;
                MBR = memory[(int) MAR];
            } else {
                System.out.println("Error: Invalid Address");
                out.println("Error: Invalid Address");
                return InvalidAddress;
            }

            IR = MBR;

            // Decode cycle
            // Decode the first word of the instruction into opcode,
            // operand 1 mode and operand 1 gpr and operand 2 mode and operand 2 gpr
            // using integer division and modulo operators
            // Five fields in the first word of any instruction is:
            // Opcode, Operand 1 mode, operand 1 GPR, Operand 2 mode, Operand 2 GPR

            // Get Opcode
            Opcode = IR / 10000;
            Remainder = IR % 10000;

            Op1Mode = Remainder / 1000;
            Remainder = Remainder % 1000;

            Op1Gpr = Remainder / 100;
            Remainder = Remainder % 100;

            Op2Mode = Remainder / 10;
            Remainder = Remainder % 10;

            Op2Gpr = Remainder;

            if (!(Op1Gpr >= 0 && Op1Gpr <= GPRs.length - 1) || !(Op2Gpr >= 0 && Op2Gpr <= GPRs.length - 1)) {
                System.out.println("Error: Invalid GPR #");
                out.println("Error: Invalid GPR #");
                return InvalidGPRNumber;
            }

            if (!(Op1Mode >= 0 && Op1Mode <= AMOUNT_MODES) || !(Op2Mode >= 0 && Op2Mode <= AMOUNT_MODES)) {
                System.out.println("Error: Invalid Mode");
                out.println("Error: Invalid Mode");
                return InvalidMode;
            }

            // Execute Cycle
            // In the execute cycle, fetch operand value(s) based on the opcode
            // since different opcode has different number of operands
            // Example:
            // Halt, Branch and System Call instructions have no operands
            // Push and Pop instructions have 1 operand
            // Conditional branch instructions have one operand
            // Add, Subtract, Multiply, Divide and Move instructions have 2 operands
            // System call has no operand

            switch ((int) Opcode) {
                case 0: // halt
                    System.out.println("Halt instruction encountered");
                    out.println("Halt instruction encountered");
                    System.out.println();
                    out.println();
                    clock += HALT_TIME;
                    TimeLeft -= HALT_TIME;
                    ProgramHalt = true;
                    status = Halt;
                    return status;
                case 1: // add
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    out2 = FetchOperand(out, Op2Mode, Op2Gpr);
                    status = out2.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    result = out1.getOpValue() + out2.getOpValue();
                    if (Op1Mode == REGISTER_MODE) {
                        GPRs[(int) Op1Gpr] = result;
                    } else if (Op1Mode == IMMEDIATE_MODE) {
                        System.out.println("Error: Immediate Mode Error");
                        out.println("Error: Immediate Mode Error");
                        return ImmediateModeError;
                    } else {
                        memory[(int) out1.getOpAddress()] = result;
                    }

                    clock += ADD_TIME;
                    TimeLeft -= ADD_TIME;
                    break;
                case 2: // subtract
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    out2 = FetchOperand(out, Op2Mode, Op2Gpr);
                    status = out2.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    result = out1.getOpValue() - out2.getOpValue();
                    if (Op1Mode == REGISTER_MODE) {
                        GPRs[(int) Op1Gpr] = result;
                    } else if (Op1Mode == IMMEDIATE_MODE) {
                        System.out.println("Error: Immediate Mode Error");
                        out.println("Error: Immediate Mode Error");
                        return ImmediateModeError;
                    } else {
                        memory[(int) out1.getOpAddress()] = result;
                    }

                    clock += SUBTRACT_TIME;
                    TimeLeft -= SUBTRACT_TIME;
                    break;
                case 3: // multiply
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    out2 = FetchOperand(out, Op2Mode, Op2Gpr);
                    status = out2.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    result = out1.getOpValue() * out2.getOpValue();
                    if (Op1Mode == REGISTER_MODE) {
                        GPRs[(int) Op1Gpr] = result;
                    } else if (Op1Mode == IMMEDIATE_MODE) {
                        System.out.println("Error: Immediate Mode Error");
                        out.println("Error: Immediate Mode Error");
                        return ImmediateModeError;
                    } else {
                        memory[(int) out1.getOpAddress()] = result;
                    }

                    clock += MULTIPLY_TIME;
                    TimeLeft -= MULTIPLY_TIME;
                    break;
                case 4: // divide
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    out2 = FetchOperand(out, Op2Mode, Op2Gpr);
                    status = out2.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    if (out2.getOpValue() == 0) {
                        System.out.println("Error: Division By Zero");
                        out.println("Error: Division By Zero");
                        return DivisionByZero;
                    }

                    result = out1.getOpValue() / out2.getOpValue();
                    if (Op1Mode == REGISTER_MODE) {
                        GPRs[(int) Op1Gpr] = result;
                    } else if (Op1Mode == IMMEDIATE_MODE) {
                        System.out.println("Error: Immediate Mode Error");
                        out.println("Error: Immediate Mode Error");
                        return ImmediateModeError;
                    } else {
                        memory[(int) out1.getOpAddress()] = result;
                    }

                    clock += DIVIDE_TIME;
                    TimeLeft -= DIVIDE_TIME;
                    break;
                case 5: // move
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    out2 = FetchOperand(out, Op2Mode, Op2Gpr);
                    status = out2.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    result = out2.getOpValue();
                    if (Op1Mode == REGISTER_MODE) {
                        GPRs[(int) Op1Gpr] = result;
                    } else if (Op1Mode == IMMEDIATE_MODE) {
                        System.out.println("Error: Immediate Mode Error");
                        out.println("Error: Immediate Mode Error");
                        return ImmediateModeError;
                    } else {
                        memory[(int) out1.getOpAddress()] = result;
                    }

                    clock += MOVE_TIME;
                    TimeLeft -= MOVE_TIME;
                    break;
                case 6: // branch
                    if (PC >= PROGRAM_AREA_START && PC <= PROGRAM_AREA_END) {
                        PC = memory[(int) PC];
                    } else {
                        System.out.println("Error: Invalid PC Value");
                        out.println("Error: Invalid PC Value");
                        return InvalidPCValue;
                    }

                    clock += BRANCH_TIME;
                    TimeLeft -= BRANCH_TIME;
                    break;
                case 7: // branch on minus
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    if (out1.getOpValue() < 0) {
                        if (PC >= PROGRAM_AREA_START && PC <= PROGRAM_AREA_END) {
                            PC = memory[(int) PC];
                        } else {
                            System.out.println("Error: Invalid PC Value");
                            out.println("Error: Invalid PC Value");
                            return InvalidPCValue;
                        }
                    } else {
                        PC++;
                    }

                    clock += BRANCH_M_TIME;
                    TimeLeft -= BRANCH_M_TIME;
                    break;
                case 8: // branch on plus
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    if (out1.getOpValue() > 0) {
                        if (PC >= PROGRAM_AREA_START && PC <= PROGRAM_AREA_END) {
                            PC = memory[(int) PC];
                        } else {
                            System.out.println("Error: Invalid PC Value");
                            out.println("Error: Invalid PC Value");
                            return InvalidPCValue;
                        }
                    } else {
                        PC++;
                    }

                    clock += BRANCH_P_TIME;
                    TimeLeft -= BRANCH_P_TIME;
                    break;
                case 9: // branch on zero
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    if (out1.getOpValue() == 0) {
                        if (PC >= PROGRAM_AREA_START && PC <= PROGRAM_AREA_END) {
                            PC = memory[(int) PC];
                        } else {
                            System.out.println("Error: Invalid PC Value");
                            out.println("Error: Invalid PC Value");
                            return InvalidPCValue;
                        }
                    } else {
                        PC++;
                    }

                    clock += BRANCH_Z_TIME;
                    TimeLeft -= BRANCH_Z_TIME;
                    break;
                case 10: // push
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    if (SP > USER_DYNAMIC_MEMORY_END) {
                        System.out.println("Error: Stack Overflow");
                        out.println("Error: Stack Overflow");
                        return StackOverflow;
                    }

                    System.out.println("Push -> " + out1.getOpValue());
                    out.println("Push -> " + out1.getOpValue());
                    SP++;
                    memory[(int) SP] = out1.getOpValue();

                    clock += PUSH_TIME;
                    TimeLeft -= PUSH_TIME;
                    break;
                case 11: // pop
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        System.out.println("Error: Fetch Error");
                        out.println("Error: Fetch Error");
                        return FetchError;
                    }

                    if (SP < USER_DYNAMIC_MEMORY_START) {
                        System.out.println("Error: Stack Underflow");
                        out.println("Error: Stack Underflow");
                        return StackUnderflow;
                    }

                    System.out.println("Pop -> " + memory[(int) SP]);
                    out.println("Pop -> " + memory[(int) SP]);
                    memory[(int) SP] = 0;
                    --SP;

                    clock += POP_TIME;
                    TimeLeft -= POP_TIME;
                    break;
                case 12: // system call
                    out1 = FetchOperand(out, Op1Mode, Op1Gpr);
                    status = out1.getStatus();
                    if (status != OK) {
                        return status;
                    }

                    status = SystemCall(out, out1.getOpValue());
                    clock += SYSTEM_CALL_TIME;
                    TimeLeft -= SYSTEM_CALL_TIME;

                    break;
                default:
                    System.out.println("Error: Invalid Opcode");
                    out.println("Error: Invalid Opcode");
                    return InvalidOpcode;
            } // end of switch

            if (TimeLeft <= 0) {
                status = TimeSliceExpired;
            }
        } // end of while
        return status;
    } // end of CPU function

    // ************************************************************
    // Function: FetchOperand
    //
    // Task Description:
    // -Create Output object to store the status, address, and value of the fetch
    // -Based on OpMode and specified register, do one of the following:
    // ---Register mode
    // ---Register deferred mode
    // ---Autoincrement mode
    // ---Autodecrement mode
    // ---Direct mode
    // ---Immediate mode
    // -Store the results in the Output object reference
    //
    // Input Parameters
    // -Out: Print to file object
    // -OpMode: Operand mode value
    // -OpReg: Operand GPR value
    //
    // Function Return Value
    // -Output object
    // ************************************************************
    public Output FetchOperand(PrintStream out, long OpMode, long OpReg) {
        Output output = new Output(0, 0, OK);

        // Fetch operand value based on the operand mode
        switch ((int) OpMode) {
            case 0: // not used
                break;
            case 1: // register mode
                output.setOpAddress(InvalidAddress);
                output.setOpValue(GPRs[(int) OpReg]);
                break;
            case 2: // register deferred mode
                output.setOpAddress(GPRs[(int) OpReg]);
                if (output.getOpAddress() >= PROGRAM_AREA_START && output.getOpAddress() <= OS_MEMORY_END) {
                    output.setOpValue(memory[(int) output.getOpAddress()]);
                } else {
                    System.out.println("Error: Invalid Address");
                    out.println("Error: Invalid Address");
                    output.setStatus(InvalidAddress);
                }
                break;
            case 3: // autoincrement mode
                output.setOpAddress(GPRs[(int) OpReg]);
                if (output.getOpAddress() >= PROGRAM_AREA_START && output.getOpAddress() <= OS_MEMORY_END) {
                    output.setOpValue(memory[(int) output.getOpAddress()]);
                } else {
                    System.out.println("Error: Invalid Address");
                    out.println("Error: Invalid Address");
                    output.setStatus(InvalidAddress);
                }
                GPRs[(int) OpReg]++;
                break;
            case 4: // autodecrement mode
                --GPRs[(int) OpReg];
                output.setOpAddress(GPRs[(int) OpReg]);
                if (output.getOpAddress() >= PROGRAM_AREA_START && output.getOpAddress() <= OS_MEMORY_END) {
                    output.setOpValue(memory[(int) output.getOpAddress()]);
                } else {
                    System.out.println("Error: Invalid Address");
                    out.println("Error: Invalid Address");
                    output.setStatus(InvalidAddress);
                }
                break;
            case 5: // direct mode
                output.setOpAddress(memory[(int) PC++]);
                if (output.getOpAddress() >= PROGRAM_AREA_START && output.getOpAddress() <= OS_MEMORY_END) {
                    output.setOpValue(memory[(int) output.getOpAddress()]);
                } else {
                    System.out.println("Error: Invalid Address");
                    out.println("Error: Invalid Address");
                    output.setStatus(InvalidAddress);
                }
                break;
            case 6: // immediate mode
                if (PC >= PROGRAM_AREA_START && PC <= OS_MEMORY_END) {
                    output.setOpAddress(InvalidAddress);
                    output.setOpValue(memory[(int) PC++]);
                } else {
                    System.out.println("Error: Invalid Address");
                    out.println("Error: Invalid Address");
                    output.setStatus(InvalidAddress);
                }
                break;
            default: // invalid mode
                System.out.println("Error: Invalid Mode");
                out.println("Error: Invalid Mode");
                output.setStatus(InvalidMode);
        } // end of switch
        return output;
    } // end of FetchOperand function

    // ************************************************************
    // Function: DumpMemory
    //
    // Task Description:
    // -Displays a string passed as one of the input parameter
    // -Displays content of GPRs, SP, PC, PSR, system Clock and
    // the content of specified memory locations in a specific format
    // -Writes the above descriptions to output file
    //
    // Input Parameters
    // -Out: Print to file object
    // -Name: String to be displayed
    // -StartAddress: Start address of memory location
    // -Size: Number of locations to dump
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void DumpMemory(PrintStream out, String name, long startAddress, long size) {
        // Name of dump
        System.out.println(name);
        out.println(name);

        // GPR, SP, and PC header
        System.out.println("GPRs:\tG0\tG1\tG2\tG3\tG4\tG5\tG6\tG7\tSP\tPC");
        out.println("GPRs:\tG0\tG1\tG2\tG3\tG4\tG5\tG6\tG7\tSP\tPC");
        System.out.printf("\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", GPRs[0], GPRs[1], GPRs[2], GPRs[3], GPRs[4],
                GPRs[5], GPRs[6], GPRs[7], SP, PC);
        out.printf("\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", GPRs[0], GPRs[1], GPRs[2], GPRs[3], GPRs[4],
                GPRs[5], GPRs[6], GPRs[7], SP, PC);

        // Address header
        System.out.println("Address\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\t+8\t+9");
        out.println("Address\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\t+8\t+9");

        long addr = startAddress;
        long endAddress = startAddress + size;

        if (!(addr >= PROGRAM_AREA_START && addr <= OS_MEMORY_END)) {
            System.out.println("Error: Invalid Start Address");
            out.println("Error: Invalid Start Address");
            return;
        }

        if (!(size > 0)) {
            System.out.println("Error: Invalid Size");
            out.println("Error: Invalid Size");
            return;
        }

        if (!(endAddress >= PROGRAM_AREA_START && endAddress <= OS_MEMORY_END)) {
            System.out.println("Error: Invalid End Address");
            out.println("Error: Invalid End Address");
            return;
        }

        while (addr < endAddress) {
            System.out.print(addr + "\t");
            out.print(addr + "\t");
            for (int i = 1; i <= 10; i++) {
                if (addr < endAddress) {
                    System.out.print(memory[(int) addr] + "\t");
                    out.print(memory[(int) addr++] + "\t");
                } else {
                    break;
                }
            }
            System.out.println();
            out.println();
        } // end of while

        System.out.println(clock);
        out.println(clock);
        System.out.println(PSR);
        out.println(PSR);
        return;
    } // end of DumpMemory function

    // ************************************************************
    // Function: CreateProcess
    //
    // Task Description:
    // -Allocate space for process control block
    // -Initialize the PCB
    // -Load the program into memory
    // -Set the PC value in PCB of the process
    // -Store stack information in the process PCB
    // -Insert PCB into ready queue
    //
    // Input Parameters
    // -Out: Print to file object
    // -Filename: Name of file to read
    // -Priority: Value to set the process priority
    //
    // Function Return Value
    // -Status
    // -MemoryAllocationFailed code
    // -LoadingProgramFailed code
    // ************************************************************
    public long CreateProcess(PrintStream out, String filename, long priority) {
        // Allocate space for proccess control block
        long PCBptr = AllocateOSMemory(out, PCBsize);

        if (PCBptr < 0) {
            System.out.println("Error: Memory Allocation Failed");
            out.println("Error: Memory Allocation Failed");
            return MemoryAllocationFailed;
        }

        // Initialize PCB: Set nextPCBlink to EOL, default priority, ready state, and
        // PID
        InitializePCB(PCBptr);

        // Load the program
        long value = AbsoluteLoader(out, filename);

        if (value < 0) {
            System.out.println("Error: Loading Program Failed");
            out.println("Error: Loading Program Failed");
            return LoadingProgramFailed;
        }

        // Store PC value in the PCB of the process
        memory[(int) PCBptr + PCindex] = value; // 20 is PC in PCB

        // Allocate stack space from user free list
        long ptr = AllocateUserMemory(out, StackSize);

        if (ptr < 0) {
            System.out.println("Error: User Memory Allocation Failed");
            out.println("Error: User Memory Allocation Failed");
            FreeOSMemory(out, PCBptr, PCBsize);
            return ptr;
        }

        // Store stack information in the PCB - SP, ptr, and size
        memory[(int) PCBptr + SPindex] = ptr + StackSize; // empty stack is high address, full is low address, 19 is
                                                          // stack
        // size in PCB
        memory[(int) PCBptr + StackStartIndex] = ptr; // 5 is stack start address in PCB
        memory[(int) PCBptr + StackSizeIndex] = StackSize; // 6 is stack size in PCB

        memory[(int) PCBptr + PriorityIndex] = priority; // 4 is priority in PCB

        DumpMemory(out, "Program Area Dump", PROGRAM_AREA_START, 90);
        System.out.println();
        out.println();

        System.out.println("PCB:");
        out.println("PCB:");
        PrintPCB(out, PCBptr);
        System.out.println();
        out.println();

        // Insert PCB into Ready Queue according to the scheduling algorithm
        InsertIntoRQ(out, PCBptr);

        return OK;
    } // end of CreateProcess

    // ************************************************************
    // Function: InitializePCB
    //
    // Task Description:
    // -Set the PCB area to 0
    // -Allocate PID and set it in PCB
    //
    // Input Parameters
    // -PCBptr: Pointer for the PCB to initialize
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void InitializePCB(long PCBptr) {
        // initialize PCB area to 0
        for (int i = 0; i < PCBsize; i++) {
            memory[(int) PCBptr + i] = 0;
        }

        // Allocate PID and set it in the PCB
        memory[(int) PCBptr + PIDindex] = ProcessID++; // second slot in PCB is PID
        memory[(int) PCBptr + PriorityIndex] = DefaultPriority; // fifth slot in PCB is Priority
        memory[(int) PCBptr + StateIndex] = ReadyState; // third slot in PCB is state
        memory[(int) PCBptr] = EOL; // first slot in PCB is pointer to next PCB
    } // end of InitializePCB

    // ************************************************************
    // Function: PrintPCB
    //
    // Task Description:
    // -Print the PCB information to the sceen and file
    // -Print the GPRs state to sceen and file
    //
    // Input Parameters
    // -Out: Print to file object
    // -PCBptr: Pointer to PCB to print
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void PrintPCB(PrintStream out, long PCBptr) {
        System.out.println("PCB address = " + PCBptr + ", Next PCB Ptr = " + memory[(int) PCBptr] + ", PID = "
                + memory[(int) PCBptr + PIDindex] + ", State = " + memory[(int) PCBptr + StateIndex] + ", PC = "
                + memory[(int) PCBptr + PCindex] + ", SP = " + memory[(int) PCBptr + SPindex] + ",");
        out.println("PCB address = " + PCBptr + ", Next PCB Ptr = " + memory[(int) PCBptr] + ", PID = "
                + memory[(int) PCBptr + PIDindex] + ", State = " + memory[(int) PCBptr + StateIndex] + ", PC = "
                + memory[(int) PCBptr + PCindex] + ", SP = " + memory[(int) PCBptr + SPindex] + ",");
        System.out.println("Priority = " + memory[(int) PCBptr + PriorityIndex] + ", Stack info: start address = "
                + memory[(int) PCBptr + StackStartIndex] + ", size = " + memory[(int) PCBptr + StackSizeIndex]);
        out.println("Priority = " + memory[(int) PCBptr + PriorityIndex] + ", Stack info: start address = "
                + memory[(int) PCBptr + StackStartIndex] + ", size = " + memory[(int) PCBptr + StackSizeIndex]);
        System.out.print("GPRs = ");
        out.print("GPRs = ");
        for (int i = 0; i < 8; i++) {
            System.out.print(GPRs[i] + " ");
            out.print(GPRs[i] + " ");
        }
        System.out.println();
        out.println();
    } // end of PrintPCB

    // ************************************************************
    // Function: PrintQueue
    //
    // Task Description:
    // -Print each of the PCBs in the queue to the screen and file
    //
    // Input Parameters
    // -Out: Print to file object
    // -Qptr: Pointer to the queue to print
    //
    // Function Return Value
    // -Status
    // ************************************************************
    public long PrintQueue(PrintStream out, long Qptr) {
        // Walk thru the queue from the given pointer until EOL
        // Print each PCB as you move from one PCB to the next
        long currentPCBptr = Qptr;

        if (currentPCBptr == EOL) {
            System.out.println("Empty List");
            out.println("Empty List");
            return OK;
        }

        // Walk thru the queue
        while (currentPCBptr != EOL) {
            PrintPCB(out, currentPCBptr);
            currentPCBptr = memory[(int) currentPCBptr + NextPCBindex];
        } // end of while

        return OK;
    } // end of PrintQueue

    // ************************************************************
    // Function: InsertIntoRQ
    //
    // Task Description:
    // -Insert PCB into RQ according to the priority
    // -Check for an invalid memory address
    //
    // Input Parameters
    // -Out: Print to file object
    // -PCBptr: Pointer to PCB to insert into RQ
    //
    // Function Return Value
    // -Status
    // -InvalidMemoryAddress
    // ************************************************************
    public long InsertIntoRQ(PrintStream out, long PCBptr) {
        // Insert PCB according to priority round robin algorithm
        // Use priority in PCB to find correct place to insert
        long previousPtr = EOL;
        long currentPtr = RQ;

        // Check for invalid PCB memory address
        if ((PCBptr < 0) || (PCBptr > OS_MEMORY_END)) {
            System.out.println("Error: Invalid Memory Address");
            out.println("Error: Invalid Memory Address");
            return InvalidMemoryAddress;
        }

        memory[(int) PCBptr + StateIndex] = ReadyState;
        memory[(int) PCBptr + NextPCBindex] = EOL;

        // RQ is empty
        if (RQ == EOL) {
            RQ = PCBptr;
            return OK;
        }

        // Walk thru RQ and find the place to insert
        // PCB will be inserted at the end of its priority
        while (currentPtr != EOL) {
            if (memory[(int) PCBptr + PriorityIndex] > memory[(int) currentPtr + PriorityIndex]) {
                // Found the place to insert
                if (previousPtr == EOL) {
                    // Enter PCB in the front of the list as first entry
                    memory[(int) PCBptr + NextPCBindex] = RQ;
                    RQ = PCBptr;
                    return OK;
                }

                // Enter PCB in the middle of the list
                memory[(int) PCBptr + NextPCBindex] = memory[(int) previousPtr + NextPCBindex];
                memory[(int) previousPtr + NextPCBindex] = PCBptr;
                return OK;
            } else {
                // PCB to be inserted has lower or equal priority to the current PCB in RQ
                // Go to the next PCB in RQ
                previousPtr = currentPtr;
                currentPtr = memory[(int) currentPtr + NextPCBindex];
            }
        } // end of while

        // Insert PCB at the end of the RQ
        memory[(int) previousPtr + NextPCBindex] = PCBptr;
        return OK;
    } // end of InsertIntoRQ

    // ************************************************************
    // Function: InsertIntoWQ
    //
    // Task Description:
    // -Insert the PCB at the front of WQ
    // -Check for invalid memory address
    //
    // Input Parameters
    // -Out: Print to file object
    // -PCBptr: Pointer to PCB to insert into WQ
    //
    // Function Return Value
    // -Status
    // -InvalidMemoryAddress
    // ************************************************************
    public long InsertIntoWQ(PrintStream out, long PCBptr) {
        // Insert the given PCB at the front of WQ
        // Check for invalid PCB memory address
        if ((PCBptr < OS_MEMORY_START) || (PCBptr > OS_MEMORY_END)) {
            System.out.println("Error: Invalid Memory Address");
            out.println("Error: Invalid Memory Address");
            return InvalidMemoryAddress;
        }

        memory[(int) PCBptr + StateIndex] = WaitingState;
        memory[(int) PCBptr + NextPCBindex] = WQ;

        WQ = PCBptr;

        return OK;
    } // end of InsertIntoWQ

    // ************************************************************
    // Function: SelectProcessFromRQ
    //
    // Task Description:
    // -Select the first entry in RQ, remove it
    // -Return the pointer to that PCB
    //
    // Input Parameters
    // -None
    //
    // Function Return Value
    // -PCBptr
    // ************************************************************
    public long SelectProcessFromRQ() {
        long PCBptr = RQ; // first entry in RQ

        if (RQ != EOL) {
            // Remove first PCB from RQ
            RQ = memory[(int) RQ + NextPCBindex];
        }

        // Set next point to EOL in the PCB
        memory[(int) PCBptr + NextPCBindex] = EOL;

        return PCBptr;
    } // end of SelectProcessFromRQ

    // ************************************************************
    // Function: SaveContext
    //
    // Task Description:
    // -Save all GPRs to PCB
    // -Save the SP and PC to PCB
    //
    // Input Parameters
    // -PCBptr: Pointer to PCB to save context into
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void SaveContext(long PCBptr) {
        // Assume PCBptr is a vaild pointer
        memory[(int) PCBptr + GPR0Index] = GPRs[0];
        memory[(int) PCBptr + GPR1Index] = GPRs[1];
        memory[(int) PCBptr + GPR2Index] = GPRs[2];
        memory[(int) PCBptr + GPR3Index] = GPRs[3];
        memory[(int) PCBptr + GPR4Index] = GPRs[4];
        memory[(int) PCBptr + GPR5Index] = GPRs[5];
        memory[(int) PCBptr + GPR6Index] = GPRs[6];
        memory[(int) PCBptr + GPR7Index] = GPRs[7];

        memory[(int) PCBptr + SPindex] = SP;
        memory[(int) PCBptr + PCindex] = PC;
    } // end of SaveContext

    // ************************************************************
    // Function: Dispatcher
    //
    // Task Description:
    // -Set the GPRs to the values stored in PCB
    // -Set the SP and PC to the values stored in PCB
    // -Set PSR to usermode
    //
    // Input Parameters
    // -PCBptr: Pointer to PCB to set GPRs, SP, and PC
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void Dispatcher(long PCBptr) {
        // PCBptr is assumed to be correct
        // Copy CPU GPR register values from given PCB into the CPU registers
        GPRs[0] = memory[(int) PCBptr + GPR0Index];
        GPRs[1] = memory[(int) PCBptr + GPR1Index];
        GPRs[2] = memory[(int) PCBptr + GPR2Index];
        GPRs[3] = memory[(int) PCBptr + GPR3Index];
        GPRs[4] = memory[(int) PCBptr + GPR4Index];
        GPRs[5] = memory[(int) PCBptr + GPR5Index];
        GPRs[6] = memory[(int) PCBptr + GPR6Index];
        GPRs[7] = memory[(int) PCBptr + GPR7Index];

        SP = memory[(int) PCBptr + SPindex];
        PC = memory[(int) PCBptr + PCindex];

        PSR = UserMode;
    } // end of Dispatcher

    // ************************************************************
    // Function: TerminateProcess
    //
    // Task Description:
    // -Return the user memory from the process
    // -Return the os memory from the process
    //
    // Input Parameters
    // -Out: Print to file object
    // -PCBptr: Pointer to PCB to free the process
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void TerminateProcess(PrintStream out, long PCBptr) {
        // Return stack memory using stack start address and stack size in the given PCB
        FreeUserMemory(out, memory[(int) PCBptr + StackStartIndex], memory[(int) PCBptr + StackSizeIndex]);

        // Return PCB memory using the PCBptr
        FreeOSMemory(out, PCBptr, PCBsize);
    } // end of TerminateProcess

    // ************************************************************
    // Function: AllocateOSMemory
    //
    // Task Description:
    // -Allocate memory from the OS free space
    //
    // Input Parameters
    // -Out: Print to file object
    // -RequestedSize: Amount of space to allocate
    //
    // Function Return Value
    // -Pointer to memory address
    // -NoFreeMemory
    // -InvalidMemorySize
    // ************************************************************
    public long AllocateOSMemory(PrintStream out, long RequestedSize) {
        // Allocate memory from OS free space
        if (OSFreeList == EOL) {
            System.out.println("Error: No free OS memory");
            out.println("Error: No free OS memory");
            return NoFreeMemory;
        }

        if (RequestedSize < 0) {
            System.out.println("Error: Invalid Size");
            out.println("Error: Invalid Size");
            return InvalidMemorySize;
        }

        long CurrentPtr = OSFreeList;
        long PreviousPtr = EOL;
        while (CurrentPtr != EOL) {
            // Check each block in the link list until block with requested memory size is
            // found
            if (memory[(int) CurrentPtr + 1] == RequestedSize) {
                // Found block with requested size. Adjust pointers
                if (CurrentPtr == OSFreeList) {
                    OSFreeList = memory[(int) CurrentPtr]; // first entry is pointer to next block
                    memory[(int) CurrentPtr] = EOL; // reset next pointer in block
                    return CurrentPtr; // return memory address
                } else {
                    memory[(int) PreviousPtr] = memory[(int) CurrentPtr]; // point to next block
                    memory[(int) CurrentPtr] = EOL; // reset next pointer in block
                    return CurrentPtr; // return memory address
                }
            } else if (memory[(int) CurrentPtr + 1] > RequestedSize) {
                // Found block with size greater than requested size
                if (CurrentPtr == OSFreeList) {
                    memory[(int) CurrentPtr + (int) RequestedSize] = memory[(int) CurrentPtr]; // move next block ptr
                    memory[(int) CurrentPtr + (int) RequestedSize + 1] = memory[(int) CurrentPtr + 1] - RequestedSize;
                    OSFreeList = CurrentPtr + RequestedSize; // address of reduced block
                    memory[(int) CurrentPtr] = EOL; // reset next pointer in block
                    return CurrentPtr; // return memory address
                } else {
                    memory[(int) CurrentPtr + (int) RequestedSize] = memory[(int) CurrentPtr]; // move next block ptr
                    memory[(int) CurrentPtr + (int) RequestedSize + 1] = memory[(int) CurrentPtr + 1] - RequestedSize;
                    memory[(int) PreviousPtr] = CurrentPtr + RequestedSize; // address of reduced block
                    memory[(int) CurrentPtr] = EOL; // reset next pointer in block
                    return CurrentPtr; // return memory address
                }
            } else {
                // look at next block
                PreviousPtr = CurrentPtr;
                CurrentPtr = memory[(int) CurrentPtr];
            }
        } // end of while

        System.out.println("Error: No free OS memory");
        out.println("Error: No free OS memory");
        return NoFreeMemory;
    } // end of AllocateOSMemory

    // ************************************************************
    // Function: FreeOSMemory
    //
    // Task Description:
    // -Free the OS memory from the ptr given and size given
    //
    // Input Parameters
    // -Out: Print to file object
    // -Ptr: Pointer to memory address to free
    // -Size: Amount of OS memory to free
    //
    // Function Return Value
    // -Status
    // -InvalidMemoryAddress
    // -InvalidMemorySize
    // ************************************************************
    public long FreeOSMemory(PrintStream out, long ptr, long size) {
        if (ptr < OS_MEMORY_START || ptr > OS_MEMORY_END) {
            System.out.println("Error: Invalid Memory Address");
            out.println("Error: Invalid Memory Address");
            return InvalidMemoryAddress;
        }

        // check for minimum allocated size
        if (size == 1) {
            size = 2; // minimum allocated size
        } else if (size < 1 || (ptr + size) >= OS_MEMORY_END) {
            // invalid size
            System.out.println("Error: Invalid Memory Size");
            out.println("Error: Invalid Memory Size");
            return InvalidMemorySize;
        }

        // return memory to OS free space, insert at beginning of list
        memory[(int) ptr] = OSFreeList;
        memory[(int) ptr + 1] = size;
        OSFreeList = ptr;

        return OK;
    } // end of FreeOSMemory

    // ************************************************************
    // Function: AllocateUserMemory
    //
    // Task Description:
    // -Allocate memory from the user free space
    //
    // Input Parameters
    // -Out: Print to file object
    // -Size: Amount of space to allocate
    //
    // Function Return Value
    // -Pointer to memory address
    // -NoFreeMemory
    // -InvalidMemorySize
    // ************************************************************
    public long AllocateUserMemory(PrintStream out, long size) {
        // Allocate memory from user free space
        if (UserFreeList == EOL) {
            System.out.println("Error: No free user memory");
            out.println("Error: No free user memory");
            return NoFreeMemory;
        }

        if (size < 0) {
            System.out.println("Error: Invalid Size");
            out.println("Error: Invalid Size");
            return InvalidMemorySize;
        }

        if (size == 1) {
            size = 2; // Minimum allocated memory is 2
        }

        long CurrentPtr = UserFreeList;
        long PreviousPtr = EOL;
        while (CurrentPtr != EOL) {
            // Check each block in the link list until block with requested memory size is
            // found
            if (memory[(int) CurrentPtr + 1] == size) {
                // Found block with requested size. Adjust pointers
                if (CurrentPtr == UserFreeList) {
                    UserFreeList = memory[(int) CurrentPtr]; // first entry is pointer to next block
                    memory[(int) CurrentPtr] = EOL; // reset next pointer in block
                    return CurrentPtr; // return memory address
                } else {
                    memory[(int) PreviousPtr] = memory[(int) CurrentPtr]; // point to next block
                    memory[(int) CurrentPtr] = EOL; // reset next pointer in block
                    return CurrentPtr; // return memory address
                }
            } else if (memory[(int) CurrentPtr + 1] > size) {
                // Found block with size greater than requested size
                if (CurrentPtr == UserFreeList) {
                    memory[(int) CurrentPtr + (int) size] = memory[(int) CurrentPtr]; // move next block ptr
                    memory[(int) CurrentPtr + (int) size + 1] = memory[(int) CurrentPtr + 1] - size;
                    UserFreeList = CurrentPtr + size; // address of reduced block
                    memory[(int) CurrentPtr] = EOL; // reset next pointer in block
                    return CurrentPtr; // return memory address
                } else {
                    memory[(int) CurrentPtr + (int) size] = memory[(int) CurrentPtr]; // move next block ptr
                    memory[(int) CurrentPtr + (int) size + 1] = memory[(int) CurrentPtr + 1] - size;
                    memory[(int) PreviousPtr] = CurrentPtr + size; // address of reduced block
                    memory[(int) CurrentPtr] = EOL; // reset next pointer in block
                    return CurrentPtr; // return memory address
                }
            } else {
                // look at next block
                PreviousPtr = CurrentPtr;
                CurrentPtr = memory[(int) CurrentPtr];
            }
        } // end of while

        System.out.println("Error: No free user memory");
        out.println("Error: No free user memory");
        return NoFreeMemory;
    } // end of AllocateUserMemory

    // ************************************************************
    // Function: FreeUserMemory
    //
    // Task Description:
    // -Free the user memory from the ptr given and size given
    //
    // Input Parameters
    // -Out: Print to file object
    // -Ptr: Pointer to memory address to free
    // -Size: Amount of user memory to free
    //
    // Function Return Value
    // -Status
    // -InvalidMemoryAddress
    // -InvalidMemorySize
    // ************************************************************
    public long FreeUserMemory(PrintStream out, long ptr, long size) {
        // return memory to user free space
        // insert the returned free block at beginning of list
        if (ptr < USER_DYNAMIC_MEMORY_START || ptr > USER_DYNAMIC_MEMORY_END) {
            System.out.println("Error: Invalid Memory Address");
            out.println("Error: Invalid Memory Address");
            return InvalidMemoryAddress;
        }

        // check for minimum allocated size
        if (size == 1) {
            size = 2; // minimum allocated size
        } else if (size < 1 || (ptr + size) >= USER_DYNAMIC_MEMORY_END) {
            // invalid size
            System.out.println("Error: Invalid Memory Size");
            out.println("Error: Invalid Memory Size");
            return InvalidMemorySize;
        }

        // return memory to OS free space, insert at beginning of list
        memory[(int) ptr] = UserFreeList;
        memory[(int) ptr + 1] = size;
        UserFreeList = ptr;

        return OK;
    } // end of FreeUserMemory

    // ************************************************************
    // Function: CheckAndProcessInterrupt
    //
    // Task Description:
    // -Prompt user to:
    // ---No interrupt
    // ---Run program
    // ---Shutdown system
    // ---Input operation
    // ---Output operation
    // -Get the ID from user
    // -Run correct interrupt based on ID
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void CheckAndProcessInterrupt(PrintStream out) {
        // Prompt and read interrupt ID
        System.out.println("0 - No interrupt");
        out.println("0 - No interrupt");
        System.out.println("1 - Run program");
        out.println("1 - Run program");
        System.out.println("2 - Shutdown system");
        out.println("2 - Shutdown system");
        System.out.println("3 - Input operation completion");
        out.println("3 - Input operation completion");
        System.out.println("4 - Output operation completion");
        out.println("4 - Output operation completion");
        System.out.print("Enter one of the interrupts above (number): ");
        out.println("Enter one of the interrupts above (number): ");
        int InterruptID = input.nextInt();
        System.out.println("Value entered -> " + InterruptID);
        out.println("Value entered -> " + InterruptID);

        // Process interrupt
        switch (InterruptID) {
            case 0: // No interrupt
                break;
            case 1: // Run program
                ISRrunProgramInterrupt(out);
                break;
            case 2: // Shutdown system
                ISRshutdownSystem(out);
                SystemShutdownStatus = true;
                break;
            case 3: // Input operation completion
                ISRinputCompletionInterrupt(out);
                break;
            case 4: // Output operation completion
                ISRoutputCompletionInterrupt(out);
                break;
            default: // Invalid interrupt ID
                System.out.println("Error: Invalid interrupt ID");
                out.println("Error: Invalid interrupt ID");
                break;
        } // end of switch
    } // end of CheckAndProcessInterrupt

    // ************************************************************
    // Function: ISRrunProgramInterrupt
    //
    // Task Description:
    // -Promt user to enter filename of file to read
    // -Create process from filename given
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void ISRrunProgramInterrupt(PrintStream out) {
        // Prompt and read filename
        System.out.print("Enter the program filename: ");
        out.println("Enter the program filename: ");
        String filename = input.next();
        out.println("Filename entered -> " + filename);

        CreateProcess(out, filename, DefaultPriority);
    } // end of ISRrunProgramInterrupt

    // ************************************************************
    // Function: ISRinputCompletionInterrupt
    //
    // Task Description:
    // -Prompt user for PID of process completing input
    // -Search and remove the PCB using PID given
    // -If found, prompt user for one character
    // -Store character in GPR1
    // -Insert process back into RQ
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void ISRinputCompletionInterrupt(PrintStream out) {
        // Prompt and read PID of the process completing input completion interrupt
        System.out.print("Enter the PID: ");
        out.print("Enter the PID: ");
        long PID = input.nextLong();
        out.println("Value entered -> " + PID);

        long WQptr = SearchAndRemovePCBfromWQ(out, PID);
        if (WQptr != EOL) {
            System.out.print("Enter one character: ");
            out.print("Enter one character: ");
            char letter = input.next().charAt(0);
            out.println("Character entered -> " + letter);

            memory[(int) WQptr + GPR1Index] = (long) letter;
            memory[(int) WQptr + StateIndex] = ReadyState;
            InsertIntoRQ(out, WQptr);
        } else {
            long RQptr = SearchRQ(out, PID);
            if (RQptr != EOL) {
                System.out.print("Enter one character: ");
                out.print("Enter one character: ");
                char letter = input.next().charAt(0);
                out.println("Character entered -> " + letter);

                memory[(int) RQptr + GPR1Index] = (long) letter;
            } else {
                System.out.println("Error: Invalid PID");
                out.println("Error: Invalid PID");
            }
        }
    } // end of ISRinputCompletionInterrupt

    // ************************************************************
    // Function: ISRoutputCompletionInterrupt
    //
    // Task Description:
    // -Prompt user for PID of process completing output
    // -Search and remove the PCB using PID given
    // -If found, print the one character
    // -Insert process into RQ
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void ISRoutputCompletionInterrupt(PrintStream out) {
        // Prompt and read PID of the process completing input completion interrupt
        System.out.print("Enter the PID: ");
        out.print("Enter the PID: ");
        long PID = input.nextLong();
        out.println("Value entered -> " + PID);

        long WQptr = SearchAndRemovePCBfromWQ(out, PID);
        if (WQptr != EOL) {
            System.out.println();
            System.out.println((char) memory[(int) WQptr + GPR1Index]);
            System.out.println();
            out.println();
            out.println((char) memory[(int) WQptr + GPR1Index]);
            out.println();
            memory[(int) WQptr + StateIndex] = ReadyState;
            InsertIntoRQ(out, WQptr);
        } else {
            long RQptr = SearchRQ(out, PID);
            if (RQptr != EOL) {
                System.out.println((char) memory[(int) RQptr + GPR1Index]);
                System.out.println();
                out.println((char) memory[(int) RQptr + GPR1Index]);
                out.println();
            } else {
                System.out.println("Error: Invalid PID");
                out.println("Error: Invalid PID");
            }
        }
    }

    // ************************************************************
    // Function: ISRshutdownSystem
    //
    // Task Description:
    // -Terminate all processes
    // -Exit OS
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -None
    // ************************************************************
    public void ISRshutdownSystem(PrintStream out) {
        // Terminate all processes in RQ one by one
        long ptr = RQ; // Set ptr to first PCB pointed by RQ

        while (ptr != EOL) {
            RQ = memory[(int) ptr + NextPCBindex];
            TerminateProcess(out, ptr);
            ptr = RQ;
        }

        // Terminate all processes in WQ one by one
        ptr = WQ;

        while (ptr != EOL) {
            WQ = memory[(int) ptr + NextPCBindex];
            TerminateProcess(out, ptr);
            ptr = WQ;
        }
    } // end of ISRshutdownSystem

    // ************************************************************
    // Function: SearchAndRemovePCBfromWQ
    //
    // Task Description:
    // -Search WQ for PCB with given PID
    // -If found, remove PCB from WQ
    //
    // Input Parameters
    // -Out: Print to file object
    // -Pid: ID for a process
    //
    // Function Return Value
    // -PCBptr
    // -EOL
    // ************************************************************
    public long SearchAndRemovePCBfromWQ(PrintStream out, long pid) {
        long currentPCBptr = WQ;
        long previousPCBptr = EOL;

        // Search WQ for a PCB that has the given pid
        // If a match is found, remove it from WQ and return the PCB pointer
        while (currentPCBptr != EOL) {
            if (memory[(int) currentPCBptr + PIDindex] == pid) {
                // Match found, remove from WQ
                if (previousPCBptr == EOL) {
                    // First PCB
                    WQ = memory[(int) currentPCBptr + NextPCBindex];
                } else {
                    // Not first PCB
                    memory[(int) previousPCBptr + NextPCBindex] = memory[(int) currentPCBptr + NextPCBindex];
                }
                memory[(int) currentPCBptr + NextPCBindex] = EOL;
                return currentPCBptr;
            }
            previousPCBptr = currentPCBptr;
            currentPCBptr = memory[(int) currentPCBptr + NextPCBindex];
        } // end of while

        // No matching PCB found
        System.out.println("PID Not Found In WQ");
        out.println("PID Not Found In WQ");
        return EOL;
    } // end of SearchAndRemovePCBfromWQ

    // ************************************************************
    // Function: SearchRQ
    //
    // Task Description:
    // -Search RQ for PCB with given PID
    //
    // Input Parameters
    // -Out: Print to file object
    // -Pid: ID for a process
    //
    // Function Return Value
    // -PCBptr
    // -EOL
    // ************************************************************
    public long SearchRQ(PrintStream out, long pid) {
        long currentPCBptr = RQ;
        long previousPCBptr = EOL;

        // Search RQ for a PCB that has the given pid
        while (currentPCBptr != EOL) {
            if (memory[(int) currentPCBptr + PIDindex] == pid) {
                // Match found
                return currentPCBptr;
            }
            previousPCBptr = currentPCBptr;
            currentPCBptr = memory[(int) currentPCBptr + NextPCBindex];
        } // end of while

        // No matching PCB found
        System.out.println("PID Not Found IN RQ");
        out.println("PID Not Found IN RQ");
        return EOL;
    } // end of SearchRQ

    // ************************************************************
    // Function: SystemCall
    //
    // Task Description:
    // -Set PSR to OSmode
    // -Run system call with given system call id
    // -Set PSR back to usermode
    //
    // Input Parameters
    // -Out: Print to file object
    // -System call id: ID of system call to use
    //
    // Function Return Value
    // -Status
    // ************************************************************
    public long SystemCall(PrintStream out, long SystemCallID) {
        // Set system mode to OS mode
        PSR = OSMode;

        long status = OK;

        switch ((int) SystemCallID) {
            case 1: // Create process - user process is creating a child process
                System.out.println("Create Process System Call Not Implemented");
                out.println("Create Process System Call Not Implemented");
                break;
            case 2: // Delete process
                System.out.println("Delete Process System Call Not Implemented");
                out.println("Delete Process System Call Not Implemented");
                break;
            case 3: // Process inquiry
                System.out.println("Process Inquiry System Call Not Implemented");
                out.println("Process Inquiry System Call Not Implemented");
                break;
            case 4: // Dynamic memory allocation
                status = MemAllocSystemCall(out);
                break;
            case 5: // Free dynamically allocated user memory
                status = MemFreeSystemCall(out);
                break;
            case 6: // Msg send
                System.out.println("Message Send System Call Not Implemented");
                out.println("Message Send System Call Not Implemented");
                break;
            case 7: // Msg receive
                System.out.println("Message Receive System Call Not Implemented");
                out.println("Message Receive System Call Not Implemented");
                break;
            case 8: // io_getc
                status = io_getcSystemCall();
                break;
            case 9: // io_putc
                status = io_putcSystemCall();
                break;
            case 10: // Time get
                System.out.println("Time Get System Call Not Implemented");
                out.println("Time Get System Call Not Implemented");
                break;
            case 11: // Time set
                System.out.println("Time Set System Call Not Implemented");
                out.println("Time Set System Call Not Implemented");
                break;
            default: // Invalid system call
                System.out.println("Error: Invalid System Call");
                out.println("Error: Invalid System Call");
                break;
        } // end of switch

        // Set system mode to user mode
        PSR = UserMode;

        return status;
    } // end of SystemCall

    // ************************************************************
    // Function: MemAllocSystemCall
    //
    // Task Description:
    // -Allocate memory from user free list
    // -Set GPR1 to pointer of allocated space
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -GPR0 (status)
    // ************************************************************
    public long MemAllocSystemCall(PrintStream out) {
        // Allocate memory from user free list
        // Return status from the function is either the address of allocated memory or
        // an error code

        long size = GPRs[2];

        // Check size of 1 and change it to 2
        if (size == 1) {
            size = 2;
        }

        GPRs[1] = AllocateUserMemory(out, size);
        if (GPRs[1] < 0) {
            GPRs[0] = GPRs[1]; // Set GPR0 to have the return status
        } else {
            GPRs[0] = OK;
        }

        System.out.println("Mem_alloc System Call");
        out.println("Mem_alloc System Call");
        System.out.println("GPR0: " + GPRs[0] + " GPR1: " + GPRs[1] + " GPR2: " + GPRs[2]);
        out.println("GPR0: " + GPRs[0] + " GPR1: " + GPRs[1] + " GPR2: " + GPRs[2]);
        System.out.println();
        out.println();

        return GPRs[0];
    } // end of MemAllocSystemCall

    // ************************************************************
    // Function: MemFreeSystemCall
    //
    // Task Description:
    // -Return allocated memory to user free list
    // -GPR1 has the pointer to memory address
    // -GPR2 has the size to free
    //
    // Input Parameters
    // -Out: Print to file object
    //
    // Function Return Value
    // -GPR0 (status)
    // ************************************************************
    public long MemFreeSystemCall(PrintStream out) {
        // Return dynamically allocated memory to the user free list
        // GPR1 has memory address and GPR2 has memory size to be released
        // Return status in GPR0

        long size = GPRs[2];

        // Check size of 1 and change it to 2
        if (size == 1) {
            size = 2;
        }

        GPRs[0] = FreeUserMemory(out, GPRs[1], size);

        System.out.println("Mem_free System Call");
        out.println("Mem_free System Call");
        System.out.println("GPR0: " + GPRs[0] + " GPR1: " + GPRs[1] + " GPR2: " + GPRs[2]);
        out.println("GPR0: " + GPRs[0] + " GPR1: " + GPRs[1] + " GPR2: " + GPRs[2]);
        System.out.println();
        out.println();

        return GPRs[0];
    } // end of MemFreeSystemCall

    // ************************************************************
    // Function: io_getcSystemCall
    //
    // Task Description:
    // -Return the StartOfInputOperation status
    //
    // Input Parameters
    // -None
    //
    // Function Return Value
    // -StartOfInputOperation
    // ************************************************************
    public long io_getcSystemCall() {
        return StartOfInputOperation;
    } // end of io_getcSystemCall

    // ************************************************************
    // Function: io_putcSystemCall
    //
    // Task Description:
    // -Return the StartOfOutputOperation status
    //
    // Input Parameters
    // -None
    //
    // Function Return Value
    // -StartOfOutputOperation
    // ************************************************************
    public long io_putcSystemCall() {
        return StartOfOutputOperation;
    } // end of io_putcSystemCall
}